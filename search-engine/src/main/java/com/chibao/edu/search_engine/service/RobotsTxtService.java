package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.entity.DomainMetadata;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Senior Level: Robots.txt Parser with intelligent caching and compliance
 * <p>
 * Features:
 * - Parses robots.txt according to RFC 9309
 * - Handles User-agent specific rules
 * - Supports Crawl-delay directive
 * - Sitemap.xml extraction
 * - Wildcard pattern matching (* and $)
 * - Rule precedence (most specific wins)
 * - Intelligent caching with TTL
 * - Async parsing to avoid blocking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotsTxtService {

    private final DomainMetadataRepository domainMetadataRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // In-memory cache for hot domains
    private final Map<String, RobotsTxtRules> rulesCache = new ConcurrentHashMap<>();

    private static final String ROBOTS_CACHE_PREFIX = "robots:";
    private static final long ROBOTS_CACHE_TTL_HOURS = 24;
    private static final String USER_AGENT = "SearchEngineBot";
    private static final String USER_AGENT_LOWERCASE = USER_AGENT.toLowerCase();
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /**
     * Represents a single Allow/Disallow rule.
     */
    @Data
    private static class Rule {
        private final String path;
        private final Pattern pattern;
        private final boolean allow;
    }

    /**
     * Container for all parsed rules for a domain.
     */
    @Data
    public static class RobotsTxtRules {
        // Holds the rules (either specific or global) that apply to our user agent.
        private List<Rule> rules = new ArrayList<>();
        private Long crawlDelayMs;
        private List<String> sitemapUrls = new ArrayList<>();
        private LocalDateTime lastFetched;
        private boolean existsOnServer;
    }

    /**
     * Check if URL is allowed to be crawled according to robots.txt
     */
    public CompletableFuture<Boolean> isAllowed(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = new URI(url);
                String domain = uri.getHost();
                String path = uri.getPath().isEmpty() ? "/" : uri.getPath();

                // Get rules for domain
                RobotsTxtRules rules = getRulesForDomain(domain);

                if (rules == null || !rules.isExistsOnServer()) {
                    // No robots.txt or fetch failed = allowed by default
                    return true;
                }

                // --- Refactored Precedence Logic (RFC 9309) ---
                // Find the most specific rule (longest path) that matches.

                Rule bestMatch = null;
                for (Rule rule : rules.getRules()) {
                    if (rule.getPattern().matcher(path).matches()) {
                        if (bestMatch == null || rule.getPath().length() > bestMatch.getPath().length()) {
                            bestMatch = rule;
                        }
                    }
                }

                if (bestMatch == null) {
                    // No matching rule = allowed
                    return true;
                }

                // The most specific rule wins
                if (bestMatch.isAllow()) {
                    log.debug("Path {} explicitly allowed by robots.txt (rule: {})", path, bestMatch.getPath());
                    return true;
                } else {
                    log.debug("Path {} disallowed by robots.txt (rule: {})", path, bestMatch.getPath());
                    return false;
                }

            } catch (Exception e) {
                log.error("Error checking robots.txt for {}", url, e);
                // On error, be conservative and allow
                return true;
            }
        });
    }

    /**
     * Get crawl delay for domain from robots.txt
     */
    public Long getCrawlDelay(String domain) {
        try {
            RobotsTxtRules rules = getRulesForDomain(domain);
            return (rules != null && rules.getCrawlDelayMs() != null) ? rules.getCrawlDelayMs() : 1000L;
        } catch (Exception e) {
            log.error("Error getting crawl delay for {}", domain, e);
            return 1000L; // Default 1 second
        }
    }

    /**
     * Get sitemap URLs from robots.txt
     */
    public List<String> getSitemaps(String domain) {
        try {
            RobotsTxtRules rules = getRulesForDomain(domain);
            return rules != null ? rules.getSitemapUrls() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error getting sitemaps for {}", domain, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get or fetch robots.txt rules for domain
     */
    private RobotsTxtRules getRulesForDomain(String domain) {
        // Check in-memory cache first
        RobotsTxtRules cached = rulesCache.get(domain);
        if (cached != null && isRecentEnough(cached.getLastFetched())) {
            return cached;
        }

        // Check Redis cache
        String redisKey = ROBOTS_CACHE_PREFIX + domain;
        String cachedContent = redisTemplate.opsForValue().get(redisKey);

        if (cachedContent != null) {
            RobotsTxtRules rules = parseRobotsTxt(cachedContent);
            rules.setExistsOnServer(true);
            rules.setLastFetched(LocalDateTime.now());
            rulesCache.put(domain, rules);
            return rules;
        }

        // Fetch from server.
        // We block (.join()) on the async fetch, but this whole method
        // is called from within an async task (see isAllowed),
        // so we are not blocking the main request thread.
        try {
            return fetchAndCacheRobotsTxt(domain).join();
        } catch (Exception e) {
            log.error("Failed to join fetchAndCacheRobotsTxt for {}", domain, e);
            return createDefaultRules();
        }
    }

    /**
     * Fetch robots.txt from server.
     * This method runs in a separate thread pool (due to @Async).
     */
    @Async
    public CompletableFuture<RobotsTxtRules> fetchAndCacheRobotsTxt(String domain) {
        // Removed CompletableFuture.supplyAsync wrapper as @Async handles the
        // threading.
        try {
            String robotsUrl = "https://" + domain + "/robots.txt";
            log.info("Fetching robots.txt from {}", robotsUrl);

            // --- Fix: Replaced deprecated URL constructor ---
            URL url = URI.create(robotsUrl).toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                // Read content
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }

                String robotsContent = content.toString();

                // Parse rules
                RobotsTxtRules rules = parseRobotsTxt(robotsContent);
                rules.setExistsOnServer(true);
                rules.setLastFetched(LocalDateTime.now());

                // Cache in Redis
                String redisKey = ROBOTS_CACHE_PREFIX + domain;
                redisTemplate.opsForValue().set(
                        redisKey,
                        robotsContent,
                        ROBOTS_CACHE_TTL_HOURS,
                        TimeUnit.HOURS);

                // Cache in memory
                rulesCache.put(domain, rules);

                // Update domain metadata
                updateDomainMetadata(domain, rules);

                log.info("Successfully fetched and cached robots.txt for {}", domain);
                return CompletableFuture.completedFuture(rules);

            } else if (responseCode == 404) {
                // No robots.txt = all allowed
                log.debug("No robots.txt found for {}", domain);
                RobotsTxtRules emptyRules = new RobotsTxtRules();
                emptyRules.setExistsOnServer(false);
                emptyRules.setLastFetched(LocalDateTime.now());
                rulesCache.put(domain, emptyRules);
                return CompletableFuture.completedFuture(emptyRules);

            } else {
                log.warn("Unexpected response code {} for robots.txt at {}", responseCode, domain);
                return CompletableFuture.completedFuture(createDefaultRules());
            }

        } catch (Exception e) {
            log.error("Failed to fetch robots.txt for {}", domain, e);
            return CompletableFuture.completedFuture(createDefaultRules());
        }
    }

    /**
     * Parse robots.txt content.
     * Refactored to reduce complexity and follow "specific agent" vs "*" logic.
     */
    private RobotsTxtRules parseRobotsTxt(String content) {
        List<Rule> globalRules = new ArrayList<>();
        List<Rule> specificRules = new ArrayList<>();
        List<String> sitemapUrls = new ArrayList<>();
        Long globalCrawlDelay = null;
        Long specificCrawlDelay = null;

        boolean foundSpecificGroup = false;
        String currentAgent = null; // Can be "*", "specific", or "other"

        for (String line : content.split("\n")) {
            // Remove comments and trim
            int commentIndex = line.indexOf('#');
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Parse directive
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String directive = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            switch (directive) {
                case "user-agent":
                    String agent = value.toLowerCase();
                    if (agent.equals("*")) {
                        currentAgent = "*";
                    } else if (agent.contains(USER_AGENT_LOWERCASE)) {
                        currentAgent = "specific";
                        foundSpecificGroup = true;
                    } else {
                        currentAgent = "other";
                    }
                    break;

                // Rules apply to the currentUserAgent
                case "disallow":
                    if (currentAgent != null && !value.isEmpty()) {
                        Rule rule = new Rule(value, createPattern(value), false);
                        if ("specific".equals(currentAgent)) {
                            specificRules.add(rule);
                        } else if ("*".equals(currentAgent)) {
                            globalRules.add(rule);
                        }
                    }
                    break;

                case "allow":
                    if (currentAgent != null && !value.isEmpty()) {
                        Rule rule = new Rule(value, createPattern(value), true);
                        if ("specific".equals(currentAgent)) {
                            specificRules.add(rule);
                        } else if ("*".equals(currentAgent)) {
                            globalRules.add(rule);
                        }
                    }
                    break;

                case "crawl-delay":
                    if (currentAgent != null) {
                        try {
                            double delaySeconds = Double.parseDouble(value);
                            long delayMs = (long) (delaySeconds * 1000);
                            if ("specific".equals(currentAgent)) {
                                specificCrawlDelay = delayMs;
                            } else if ("*".equals(currentAgent)) {
                                globalCrawlDelay = delayMs;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid crawl-delay value: {}", value);
                        }
                    }
                    break;

                case "sitemap":
                    // Sitemaps are global, regardless of user-agent
                    sitemapUrls.add(value);
                    break;
            }
        }

        // --- Select the correct ruleset ---
        RobotsTxtRules rules = new RobotsTxtRules();
        if (foundSpecificGroup) {
            // If a group for our specific agent was found, use its rules
            rules.setRules(specificRules);
            rules.setCrawlDelayMs(specificCrawlDelay);
        } else {
            // Otherwise, fall back to the global "*" rules
            rules.setRules(globalRules);
            rules.setCrawlDelayMs(globalCrawlDelay);
        }

        rules.setSitemapUrls(sitemapUrls);

        // If no crawl delay was specified, use the global one (if it exists)
        if (rules.getCrawlDelayMs() == null) {
            rules.setCrawlDelayMs(globalCrawlDelay);
        }

        return rules;
    }

    /**
     * Create regex pattern from robots.txt path
     * Supports wildcards: * (matches any sequence) and $ (end of path)
     */
    private Pattern createPattern(String path) {
        // Escape special regex characters except * and $
        String regex = path
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("+", "\\+")
                .replace("|", "\\|")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}");

        // Convert robots.txt wildcards to regex
        regex = regex.replace("*", ".*");

        // Handle $ (end of path)
        if (regex.endsWith("$")) {
            regex = regex.substring(0, regex.length() - 1) + "$";
        } else {
            // If no $, pattern can match prefix
            regex = regex + ".*";
        }

        // Anchor at start
        regex = "^" + regex;

        return Pattern.compile(regex);
    }

    /**
     * Update domain metadata with robots.txt info
     */
    private void updateDomainMetadata(String domain, RobotsTxtRules rules) {
        try {
            DomainMetadata metadata = domainMetadataRepository.findByDomain(domain)
                    .orElse(DomainMetadata.builder()
                            .domain(domain)
                            .maxConcurrentRequests(5)
                            .isBlocked(false)
                            .totalPagesCrawled(0)
                            .totalFailures(0)
                            .build());

            // Update crawl delay
            if (rules.getCrawlDelayMs() != null) {
                metadata.setCrawlDelayMs(rules.getCrawlDelayMs());
            } else {
                metadata.setCrawlDelayMs(1000L); // Default 1 second
            }

            // Store robots.txt content (just as a simple string of rules)
            List<String> ruleStrings = new ArrayList<>();
            for (Rule rule : rules.getRules()) {
                ruleStrings.add((rule.isAllow() ? "Allow: " : "Disallow: ") + rule.getPath());
            }
            metadata.setRobotsTxtContent(String.join("\n", ruleStrings));

            domainMetadataRepository.save(metadata);

        } catch (Exception e) {
            log.error("Failed to update domain metadata for {}", domain, e);
        }
    }

    /**
     * Check if cached rules are recent enough
     */
    private boolean isRecentEnough(LocalDateTime lastFetched) {
        if (lastFetched == null) {
            return false;
        }
        return lastFetched.isAfter(LocalDateTime.now().minusHours(ROBOTS_CACHE_TTL_HOURS));
    }

    /**
     * Create default permissive rules
     */
    private RobotsTxtRules createDefaultRules() {
        RobotsTxtRules rules = new RobotsTxtRules();
        rules.setExistsOnServer(false); // Indicates fetch failed or 404
        rules.setCrawlDelayMs(1000L);
        rules.setLastFetched(LocalDateTime.now());
        return rules;
    }

    /**
     * Clear cache for domain (useful for testing or force refresh)
     */
    public void clearCache(String domain) {
        rulesCache.remove(domain);
        String redisKey = ROBOTS_CACHE_PREFIX + domain;
        redisTemplate.delete(redisKey);
        log.info("Cleared robots.txt cache for {}", domain);
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("in_memory_cache_size", rulesCache.size());
        stats.put("cached_domains", rulesCache.keySet());

        // Get Redis cache size
        Set<String> redisKeys = redisTemplate.keys(ROBOTS_CACHE_PREFIX + "*");
        stats.put("redis_cache_size", (redisKeys != null) ? redisKeys.size() : 0);

        return stats;
    }
}