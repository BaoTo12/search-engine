package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.config.KafkaTopics;
import com.chibao.edu.search_engine.dto.request.CrawlRequest;
import com.chibao.edu.search_engine.entity.CrawlUrl;
import com.chibao.edu.search_engine.entity.DomainMetadata;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlSchedulerService {

    private final CrawlUrlRepository crawlUrlRepository;
    private final DomainMetadataRepository domainMetadataRepository;
    private final KafkaTemplate<String, CrawlRequest> crawlRequestKafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_DEPTH = 3;
    private static final String URL_VISITED_KEY_PREFIX = "visited:";
    private static final String DOMAIN_RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Add seed URLs to the crawl queue
     */
    @Transactional
    public void addSeedUrls(List<String> urls) {
        for (String url : urls) {
            try {
                String normalizedUrl = normalizeUrl(url);
                String urlHash = hashUrl(normalizedUrl);
                String domain = extractDomain(normalizedUrl);

                // Check if already exists
                if (crawlUrlRepository.existsByUrlHash(urlHash)) {
                    log.debug("URL already exists: {}", normalizedUrl);
                    continue;
                }

                CrawlUrl crawlUrl = CrawlUrl.builder()
                        .url(normalizedUrl)
                        .urlHash(urlHash)
                        .domain(domain)
                        .status(CrawlStatus.PENDING)
                        .depth(0)
                        .priority(10) // High priority for seeds
                        .build();

                crawlUrlRepository.save(crawlUrl);
                log.info("Added seed URL: {}", normalizedUrl);

            } catch (Exception e) {
                log.error("Failed to add seed URL: {}", url, e);
            }
        }
    }

    /**
     * Scheduled task to dispatch pending URLs to Kafka
     * Runs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void dispatchPendingUrls() {
        log.debug("Dispatching pending URLs...");

        List<CrawlUrl> pendingUrls = crawlUrlRepository
                .findByStatusOrderByPriorityDesc(CrawlStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (pendingUrls.isEmpty()) {
            log.debug("No pending URLs to dispatch");
            return;
        }

        int dispatched = 0;
        for (CrawlUrl crawlUrl : pendingUrls) {
            try {
                // Check rate limiting for domain
                if (isDomainRateLimited(crawlUrl.getDomain())) {
                    log.debug("Domain {} is rate-limited, skipping", crawlUrl.getDomain());
                    continue;
                }

                // Check max depth
                if (crawlUrl.getDepth() > MAX_DEPTH) {
                    crawlUrl.setStatus(CrawlStatus.BLOCKED);
                    crawlUrlRepository.save(crawlUrl);
                    continue;
                }

                // Create crawl request
                CrawlRequest request = CrawlRequest.builder()
                        .url(crawlUrl.getUrl())
                        .domain(crawlUrl.getDomain())
                        .depth(crawlUrl.getDepth())
                        .priority(crawlUrl.getPriority())
                        .timestamp(LocalDateTime.now())
                        .build();

                // Send to Kafka
                crawlRequestKafkaTemplate.send(KafkaTopics.CRAWL_REQUESTS, crawlUrl.getDomain(), request);

                // Update status
                crawlUrl.setStatus(CrawlStatus.IN_PROGRESS);
                crawlUrl.setLastCrawlAttempt(LocalDateTime.now());
                crawlUrlRepository.save(crawlUrl);

                // Update rate limit
                updateDomainRateLimit(crawlUrl.getDomain());

                dispatched++;

            } catch (Exception e) {
                log.error("Failed to dispatch URL: {}", crawlUrl.getUrl(), e);
            }
        }

        log.info("Dispatched {} URLs for crawling", dispatched);
    }

    /**
     * Retry failed URLs (runs every hour)
     */
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void retryFailedUrls() {
        log.info("Retrying failed URLs...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        List<CrawlUrl> failedUrls = crawlUrlRepository
                .findByStatusAndLastCrawlAttemptBefore(
                        CrawlStatus.FAILED,
                        cutoffTime,
                        PageRequest.of(0, 50)
                );

        int retried = 0;
        for (CrawlUrl url : failedUrls) {
            if (url.getFailureCount() < 3) {
                url.setStatus(CrawlStatus.PENDING);
                url.setPriority(Math.max(1, url.getPriority() - 1)); // Reduce priority
                crawlUrlRepository.save(url);
                retried++;
            }
        }

        log.info("Queued {} failed URLs for retry", retried);
    }

    /**
     * Check if domain is rate-limited
     */
    private boolean isDomainRateLimited(String domain) {
        String key = DOMAIN_RATE_LIMIT_PREFIX + domain;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return false;
        }

        // Get domain metadata for crawl delay
        DomainMetadata metadata = domainMetadataRepository.findByDomain(domain)
                .orElse(DomainMetadata.builder()
                        .domain(domain)
                        .crawlDelayMs(1000L)
                        .maxConcurrentRequests(5)
                        .isBlocked(false)
                        .totalPagesCrawled(0)
                        .totalFailures(0)
                        .build());

        return metadata.getIsBlocked();
    }

    /**
     * Update domain rate limit in Redis
     */
    private void updateDomainRateLimit(String domain) {
        DomainMetadata metadata = domainMetadataRepository.findByDomain(domain)
                .orElse(DomainMetadata.builder()
                        .domain(domain)
                        .crawlDelayMs(1000L)
                        .maxConcurrentRequests(5)
                        .isBlocked(false)
                        .totalPagesCrawled(0)
                        .totalFailures(0)
                        .build());

        String key = DOMAIN_RATE_LIMIT_PREFIX + domain;
        redisTemplate.opsForValue().set(
                key,
                "1",
                metadata.getCrawlDelayMs(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Normalize URL (remove fragments, sort params, etc.)
     */
    private String normalizeUrl(String url) {
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
        url = url.replaceAll("[?&](utm_[^&]+|fbclid=[^&]+|gclid=[^&]+)", "");

        return url;
    }

    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            String domain = url.replaceFirst("https?://", "").split("/")[0];
            return domain.toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Generate SHA-256 hash of URL for deduplication
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
}

