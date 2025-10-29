package com.chibao.edu.search_engine.controller;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import com.chibao.edu.search_engine.repository.WebPageRepository;
import com.chibao.edu.search_engine.service.CrawlSchedulerService;
import com.chibao.edu.search_engine.service.IndexerService;
import com.chibao.edu.search_engine.service.LinkDiscoveryService;
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
    public ResponseEntity<Map<String, String>> updatePageRank() {
        // Run asynchronously
        new Thread(() -> indexerService.updatePageRankScores()).start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "PageRank update started in background");

        return ResponseEntity.accepted().body(response);
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