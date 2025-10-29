package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.config.KafkaTopics;
import com.chibao.edu.search_engine.dto.response.LinkDiscoveryMessage;
import com.chibao.edu.search_engine.entity.CrawlUrl;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkDiscoveryService {

    private final CrawlUrlRepository crawlUrlRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VISITED_URLS_KEY = "visited_urls";
    private static final int MAX_DEPTH = 3;
    private static final int BATCH_SIZE = 50;

    // Blocked domains/patterns
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "facebook.com", "twitter.com", "instagram.com",
            "youtube.com", "reddit.com", "pinterest.com"
    );

    /**
     * Process discovered links from crawled pages
     */
    @KafkaListener(
            topics = KafkaTopics.LINK_DISCOVERIES,
            containerFactory = "linkDiscoveryListenerFactory"
    )
    @Transactional
    public void processDiscoveredLinks(LinkDiscoveryMessage message, Acknowledgment ack) {
        log.info("Processing {} discovered links from depth {}",
                message.getUrls().size(), message.getSourceDepth());

        try {
            List<String> validUrls = message.getUrls().stream()
                    .map(this::normalizeUrl)
                    .filter(this::isValidUrl)
                    .filter(url -> !isBlocked(url))
                    .filter(url -> !isAlreadyVisited(url))
                    .distinct()
                    .limit(BATCH_SIZE)
                    .collect(Collectors.toList());

            int added = 0;
            for (String url : validUrls) {
                try {
                    if (addToFrontier(url, message.getSourceDepth() + 1)) {
                        markAsVisited(url);
                        added++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to add URL to frontier: {}", url, e);
                }
            }

            log.info("Added {} new URLs to frontier (from {} candidates)",
                    added, message.getUrls().size());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing link discovery message", e);
            ack.acknowledge(); // Acknowledge anyway to avoid reprocessing
        }
    }

    /**
     * Add URL to crawl frontier
     */
    private boolean addToFrontier(String url, int depth) {
        try {
            // Check depth limit
            if (depth > MAX_DEPTH) {
                return false;
            }

            String urlHash = hashUrl(url);
            String domain = extractDomain(url);

            // Check if already exists
            if (crawlUrlRepository.existsByUrlHash(urlHash)) {
                return false;
            }

            // Calculate priority based on depth and domain
            int priority = calculatePriority(url, domain, depth);

            CrawlUrl crawlUrl = CrawlUrl.builder()
                    .url(url)
                    .urlHash(urlHash)
                    .domain(domain)
                    .status(CrawlStatus.PENDING)
                    .depth(depth)
                    .priority(priority)
                    .build();

            crawlUrlRepository.save(crawlUrl);
            return true;

        } catch (Exception e) {
            log.error("Failed to add URL to frontier: {}", url, e);
            return false;
        }
    }

    /**
     * Normalize URL for consistency
     */
    private String normalizeUrl(String url) {
        try {
            url = url.toLowerCase().trim();

            // Remove fragment
            int fragmentIndex = url.indexOf('#');
            if (fragmentIndex != -1) {
                url = url.substring(0, fragmentIndex);
            }

            // Remove trailing slash
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            // Remove common tracking parameters
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query != null) {
                String cleanQuery = Arrays.stream(query.split("&"))
                        .filter(param -> !param.startsWith("utm_"))
                        .filter(param -> !param.startsWith("fbclid="))
                        .filter(param -> !param.startsWith("gclid="))
                        .filter(param -> !param.startsWith("ref="))
                        .collect(Collectors.joining("&"));

                if (cleanQuery.isEmpty()) {
                    url = url.split("\\?")[0];
                } else {
                    url = url.split("\\?")[0] + "?" + cleanQuery;
                }
            }

            // Remove www prefix for consistency
            url = url.replaceFirst("://www\\.", "://");

            return url;

        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Validate URL format and protocol
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Must be HTTP or HTTPS
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Must have valid domain
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        // Avoid non-HTML resources
        if (url.matches(".*\\.(jpg|jpeg|png|gif|pdf|zip|exe|mp4|mp3|avi|doc|docx|xls|xlsx|ppt|pptx)$")) {
            return false;
        }

        // Avoid very long URLs (likely spam or malformed)
        if (url.length() > 500) {
            return false;
        }

        return true;
    }

    /**
     * Check if domain is blocked
     */
    private boolean isBlocked(String url) {
        try {
            String domain = extractDomain(url);

            // Check exact domain match
            if (BLOCKED_DOMAINS.contains(domain)) {
                return true;
            }

            // Check if subdomain of blocked domain
            for (String blocked : BLOCKED_DOMAINS) {
                if (domain.endsWith("." + blocked)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            return true; // Block if can't parse
        }
    }

    /**
     * Check if URL already visited (using Redis)
     */
    private boolean isAlreadyVisited(String url) {
        try {
            String urlHash = hashUrl(url);
            return Boolean.TRUE.equals(
                    redisTemplate.opsForSet().isMember(VISITED_URLS_KEY, urlHash)
            );
        } catch (Exception e) {
            log.error("Failed to check visited status: {}", url, e);
            return false;
        }
    }

    /**
     * Mark URL as visited in Redis
     */
    private void markAsVisited(String url) {
        try {
            String urlHash = hashUrl(url);
            redisTemplate.opsForSet().add(VISITED_URLS_KEY, urlHash);

            // Set TTL for visited URLs (30 days)
            redisTemplate.expire(VISITED_URLS_KEY, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to mark URL as visited: {}", url, e);
        }
    }

    /**
     * Calculate priority based on various factors
     */
    private int calculatePriority(String url, String domain, int depth) {
        int priority = 5; // Base priority

        // Decrease priority with depth
        priority -= depth;

        // Increase priority for common domains
        if (domain.endsWith(".edu") || domain.endsWith(".gov")) {
            priority += 2;
        }

        // Increase priority for news sites
        if (url.contains("/news/") || url.contains("/article/")) {
            priority += 1;
        }

        // Ensure priority is in valid range [1, 10]
        return Math.max(1, Math.min(10, priority));
    }

    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Generate SHA-256 hash for URL
     */
    private String hashUrl(String url) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Get frontier statistics
     */
    public Map<String, Object> getFrontierStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pending", crawlUrlRepository.countByStatus(CrawlStatus.PENDING));
        stats.put("in_progress", crawlUrlRepository.countByStatus(CrawlStatus.IN_PROGRESS));
        stats.put("completed", crawlUrlRepository.countByStatus(CrawlStatus.COMPLETED));
        stats.put("failed", crawlUrlRepository.countByStatus(CrawlStatus.FAILED));
        stats.put("total_visited", redisTemplate.opsForSet().size(VISITED_URLS_KEY));

        return stats;
    }
}