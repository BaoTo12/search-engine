package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.strategy.URLPrioritizationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * URL Frontier Service - Manages the URL queue with pluggable prioritization
 * strategies.
 * 
 * The URL frontier is a priority queue that determines which URLs to crawl
 * next.
 * Different strategies can be used based on crawling objectives:
 * - BFS: Breadth-first exploration
 * - Best-First: Quality-focused crawling
 * - OPIC: Real-time importance calculation
 * - Focused: Topic-specific crawling
 * 
 * Uses Redis Sorted Sets for distributed priority queue.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class URLFrontierService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PageRankService pageRankService;

    @Qualifier("bfsStrategy")
    private final URLPrioritizationStrategy bfsStrategy;

    @Qualifier("bestFirstStrategy")
    private final URLPrioritizationStrategy bestFirstStrategy;

    @Qualifier("opicStrategy")
    private final URLPrioritizationStrategy opicStrategy;

    @Qualifier("focusedStrategy")
    private final URLPrioritizationStrategy focusedStrategy;

    private static final String FRONTIER_KEY = "crawler:url_frontier";
    private static final String STRATEGY_KEY = "crawler:strategy";

    private URLPrioritizationStrategy currentStrategy = null;

    /**
     * Add URL to the frontier with priority based on current strategy.
     */
    public void addUrl(String url, int depth, double domainAuthority, double changeFrequency) {
        URLPrioritizationStrategy strategy = getCurrentStrategy();
        double pageRank = pageRankService.getPageRank(url);

        double priority = strategy.calculatePriority(
                url, depth, pageRank, domainAuthority, changeFrequency);

        // Add to Redis sorted set (higher score = higher priority)
        redisTemplate.opsForZSet().add(FRONTIER_KEY, url, priority);

        log.debug("Added URL to frontier: {} with priority {} using strategy: {}",
                url, priority, strategy.getStrategyName());
    }

    /**
     * Get next URL to crawl (highest priority).
     */
    public String getNextUrl() {
        // Get highest scoring URL
        Set<String> urls = redisTemplate.opsForZSet().reverseRange(FRONTIER_KEY, 0, 0);

        if (urls != null && !urls.isEmpty()) {
            String url = urls.iterator().next();
            // Remove from frontier
            redisTemplate.opsForZSet().remove(FRONTIER_KEY, url);
            return url;
        }

        return null;
    }

    /**
     * Get next N URLs to crawl (batch processing).
     */
    public List<String> getNextUrls(int count) {
        Set<String> urls = redisTemplate.opsForZSet().reverseRange(FRONTIER_KEY, 0, count - 1);

        if (urls != null && !urls.isEmpty()) {
            // Remove from frontier
            for (String url : urls) {
                redisTemplate.opsForZSet().remove(FRONTIER_KEY, url);
            }
            return new ArrayList<>(urls);
        }

        return Collections.emptyList();
    }

    /**
     * Get frontier size.
     */
    public long getSize() {
        Long size = redisTemplate.opsForZSet().size(FRONTIER_KEY);
        return size != null ? size : 0;
    }

    /**
     * Clear the frontier (for testing).
     */
    public void clear() {
        redisTemplate.delete(FRONTIER_KEY);
        log.info("Cleared URL frontier");
    }

    /**
     * Change the prioritization strategy.
     */
    public void setStrategy(String strategyName) {
        URLPrioritizationStrategy newStrategy = switch (strategyName.toLowerCase()) {
            case "bfs" -> bfsStrategy;
            case "best-first", "bestfirst" -> bestFirstStrategy;
            case "opic" -> opicStrategy;
            case "focused" -> focusedStrategy;
            default -> {
                log.warn("Unknown strategy: {}, defaulting to BFS", strategyName);
                yield bfsStrategy;
            }
        };

        currentStrategy = newStrategy;
        redisTemplate.opsForValue().set(STRATEGY_KEY, strategyName);
        log.info("Changed URL frontier strategy to: {}", newStrategy.getStrategyName());

        // Recalculate priorities for existing URLs
        recalculatePriorities();
    }

    /**
     * Get current strategy name.
     */
    public String getCurrentStrategyName() {
        return getCurrentStrategy().getStrategyName();
    }

    /**
     * Get current strategy (with fallback).
     */
    private URLPrioritizationStrategy getCurrentStrategy() {
        if (currentStrategy == null) {
            String savedStrategy = redisTemplate.opsForValue().get(STRATEGY_KEY);
            if (savedStrategy != null) {
                setStrategy(savedStrategy);
            } else {
                currentStrategy = bfsStrategy; // Default
            }
        }
        return currentStrategy;
    }

    /**
     * Recalculate priorities when strategy changes.
     */
    private void recalculatePriorities() {
        log.info("Recalculating priorities for existing URLs...");

        // Get all URLs with their scores
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> urlsWithScores = redisTemplate
                .opsForZSet().rangeWithScores(FRONTIER_KEY, 0, -1);

        if (urlsWithScores == null || urlsWithScores.isEmpty()) {
            return;
        }

        // Clear and re-add with new priorities
        redisTemplate.delete(FRONTIER_KEY);

        URLPrioritizationStrategy strategy = getCurrentStrategy();

        for (var tuple : urlsWithScores) {
            String url = tuple.getValue();
            if (url != null) {
                // Use default values for recalculation
                // In production, these would be stored with the URL
                addUrl(url, 1, 0.5, 1.0);
            }
        }

        log.info("Recalculated priorities for {} URLs", urlsWithScores.size());
    }

    /**
     * Get frontier statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", getSize());
        stats.put("strategy", getCurrentStrategyName());

        // Get score distribution
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> top10 = redisTemplate.opsForZSet()
                .reverseRangeWithScores(FRONTIER_KEY, 0, 9);

        if (top10 != null) {
            List<Map<String, Object>> topUrls = new ArrayList<>();
            for (var tuple : top10) {
                Map<String, Object> urlInfo = new HashMap<>();
                urlInfo.put("url", tuple.getValue());
                urlInfo.put("priority", tuple.getScore());
                topUrls.add(urlInfo);
            }
            stats.put("top10", topUrls);
        }

        return stats;
    }
}
