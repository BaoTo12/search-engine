package com.chibao.edu.search_engine.domain.crawling.strategy;

/**
 * Strategy interface for URL prioritization.
 * Multiple implementations can exist (BFS, BestFirst, OPIC, etc.)
 */
public interface PrioritizationStrategy {

    double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority);

    String getName();
}
