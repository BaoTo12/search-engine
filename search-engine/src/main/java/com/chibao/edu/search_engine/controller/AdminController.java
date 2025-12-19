package com.chibao.edu.search_engine.controller;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import com.chibao.edu.search_engine.repository.elasticsearch.WebPageRepository;
import com.chibao.edu.search_engine.service.*;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API", description = "Administrative endpoints for crawler management")
public class AdminController {

    private final CrawlSchedulerService crawlSchedulerService;
    private final IndexerService indexerService;
    private final LinkDiscoveryService linkDiscoveryService;
    private final CrawlUrlRepository crawlUrlRepository;
    private final DomainMetadataRepository domainMetadataRepository;
    private final WebPageRepository webPageRepository;

    // New services
    private final URLFrontierService urlFrontierService;
    private final PageRankService pageRankService;
    private final BloomFilterService bloomFilterService;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    /**
     * Add seed URLs to start crawling
     */
    @PostMapping("/crawl/seeds")
    @Operation(summary = "Add seed URLs", description = "Add initial URLs to start the crawl")
    public ResponseEntity<Map<String, Object>> addSeedUrls(@RequestBody List<String> urls) {
        crawlSchedulerService.addSeedUrls(urls);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Seed URLs added successfully");
        response.put("count", urls.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get crawler statistics
     */
    @GetMapping("/stats/crawler")
    @Operation(summary = "Get crawler statistics", description = "Retrieve current crawler status and statistics")
    public ResponseEntity<Map<String, Object>> getCrawlerStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pending", crawlUrlRepository.countByStatus(CrawlStatus.PENDING));
        stats.put("in_progress", crawlUrlRepository.countByStatus(CrawlStatus.IN_PROGRESS));
        stats.put("completed", crawlUrlRepository.countByStatus(CrawlStatus.COMPLETED));
        stats.put("failed", crawlUrlRepository.countByStatus(CrawlStatus.FAILED));
        stats.put("blocked", crawlUrlRepository.countByStatus(CrawlStatus.BLOCKED));
        stats.put("rate_limited", crawlUrlRepository.countByStatus(CrawlStatus.RATE_LIMITED));
        stats.put("total_domains", domainMetadataRepository.count());
        stats.put("frontier_stats", linkDiscoveryService.getFrontierStats());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get indexer statistics
     */
    @GetMapping("/stats/indexer")
    @Operation(summary = "Get indexer statistics", description = "Retrieve indexing statistics")
    public ResponseEntity<Map<String, Object>> getIndexerStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_indexed", webPageRepository.count());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get domain-specific statistics
     */
    @GetMapping("/stats/domain/{domain}")
    @Operation(summary = "Get domain statistics", description = "Retrieve statistics for a specific domain")
    public ResponseEntity<Map<String, Object>> getDomainStats(@PathVariable String domain) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_pages", webPageRepository.countByDomain(domain));
        stats.put("metadata", domainMetadataRepository.findByDomain(domain));

        return ResponseEntity.ok(stats);
    }

    /**
     * Trigger PageRank recalculation
     */
    @PostMapping("/indexer/pagerank/update")
    @Operation(summary = "Update PageRank scores", description = "Trigger PageRank score recalculation")
    public ResponseEntity<Map<String, Object>> updatePageRank() {
        // Run asynchronously
        new Thread(() -> {
            int pagesProcessed = pageRankService.calculatePageRank();
        }).start();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "PageRank calculation started in background");
        response.put("status", "PROCESSING");

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get PageRank statistics
     */
    @GetMapping("/pagerank/stats")
    @Operation(summary = "Get PageRank statistics", description = "Retrieve PageRank statistics and top pages")
    public ResponseEntity<Map<String, Object>> getPageRankStats() {
        return ResponseEntity.ok(pageRankService.getStatistics());
    }

    /**
     * Get URL Frontier statistics
     */
    @GetMapping("/frontier/stats")
    @Operation(summary = "Get URL Frontier statistics", description = "Retrieve frontier size and strategy info")
    public ResponseEntity<Map<String, Object>> getFrontierStats() {
        return ResponseEntity.ok(urlFrontierService.getStatistics());
    }

    /**
     * Change URL Frontier strategy
     */
    @PostMapping("/frontier/strategy")
    @Operation(summary = "Change frontier strategy", description = "Change URL prioritization strategy (bfs|best-first|opic|focused)")
    public ResponseEntity<Map<String, String>> changeFrontierStrategy(@RequestParam String strategy) {
        urlFrontierService.setStrategy(strategy);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Strategy changed successfully");
        response.put("newStrategy", urlFrontierService.getCurrentStrategyName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get Bloom Filter statistics
     */
    @GetMapping("/deduplication/stats")
    @Operation(summary = "Get deduplication statistics", description = "Retrieve Bloom Filter statistics")
    public ResponseEntity<Map<String, Object>> getDeduplicationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("approximateElementCount", bloomFilterService.getApproximateElementCount());
        stats.put("expectedFalsePositiveProbability", bloomFilterService.getExpectedFalsePositiveProbability());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get rate limit status for a domain
     */
    @GetMapping("/rate-limit/{domain}")
    @Operation(summary = "Get rate limit status", description = "Check rate limit status for a specific domain")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(@PathVariable String domain) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("domain", domain);
        stats.put("currentTokens", tokenBucketRateLimiter.getCurrentTokens(domain));
        stats.put("waitTimeMs", tokenBucketRateLimiter.getWaitTimeMs(domain));
        stats.put("canCrawl", tokenBucketRateLimiter.tryAcquire(domain));

        return ResponseEntity.ok(stats);
    }

    /**
     * Reset rate limit for a domain
     */
    @PostMapping("/rate-limit/{domain}/reset")
    @Operation(summary = "Reset rate limit", description = "Reset rate limit for a specific domain")
    public ResponseEntity<Map<String, String>> resetRateLimit(@PathVariable String domain) {
        tokenBucketRateLimiter.reset(domain);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rate limit reset successfully");
        response.put("domain", domain);

        return ResponseEntity.ok(response);
    }

    /**
     * Get system health status
     */
    @GetMapping("/health")
    @Operation(summary = "System health check", description = "Check health of all system components")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check database connectivity
            long crawlUrlCount = crawlUrlRepository.count();
            health.put("database", Map.of("status", "UP", "url_count", crawlUrlCount));
        } catch (Exception e) {
            health.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        try {
            // Check Elasticsearch connectivity
            long indexedCount = webPageRepository.count();
            health.put("elasticsearch", Map.of("status", "UP", "document_count", indexedCount));
        } catch (Exception e) {
            health.put("elasticsearch", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        health.put("overall_status", "UP");

        return ResponseEntity.ok(health);
    }
}