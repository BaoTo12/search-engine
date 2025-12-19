package com.chibao.edu.search_engine.strategy;

import org.springframework.stereotype.Component;

/**
 * Breadth-First Search (BFS) crawling strategy.
 * 
 * Crawls pages level by level from seed URLs:
 * - Depth 0 (seeds) crawled first
 * - Then all depth 1 pages
 * - Then all depth 2 pages, etc.
 * 
 * Pros:
 * - Finds important pages quickly (closer to seeds)
 * - Good coverage of website structure
 * - Simple and predictable
 * 
 * Cons:
 * - Can get stuck on large sites
 * - Doesn't consider page importance
 * 
 * Use case: General-purpose web crawling
 */
@Component("bfsStrategy")
public class BFSStrategy implements URLPrioritizationStrategy {

    @Override
    public double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority,
            double changeFrequency) {
        // Priority = -depth (lower depth = higher priority)
        // Negate so that depth 0 has highest score
        // Add small constant to handle depth 0
        return 1000.0 - depth;
    }

    @Override
    public String getStrategyName() {
        return "BFS (Breadth-First Search)";
    }
}
