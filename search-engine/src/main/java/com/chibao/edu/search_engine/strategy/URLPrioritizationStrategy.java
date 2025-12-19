package com.chibao.edu.search_engine.strategy;

/**
 * Strategy interface for URL prioritization in the crawler frontier.
 * Different strategies can be implemented for different crawling objectives:
 * - BFS: Breadth-first exploration
 * - Best-First: Priority-based on PageRank/authority
 * - OPIC: Online Page Importance Computation
 * - Focused: Topic-specific crawling
 */
public interface URLPrioritizationStrategy {

    /**
     * Calculate priority score for a URL.
     * Higher score = higher priority = crawled sooner.
     *
     * @param url             The URL to prioritize
     * @param depth           Crawl depth (0 = seed URL)
     * @param pageRank        PageRank score (0.0 - 1.0)
     * @param domainAuthority Domain authority score (0.0 - 1.0)
     * @param changeFrequency Expected change frequency (pages/day)
     * @return Priority score (higher = more important)
     */
    double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority,
            double changeFrequency);

    /**
     * Get the strategy name for monitoring and logging.
     */
    String getStrategyName();
}
