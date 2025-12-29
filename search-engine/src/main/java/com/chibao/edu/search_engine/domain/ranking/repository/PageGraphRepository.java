package com.chibao.edu.search_engine.domain.ranking.repository;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for PageGraph operations.
 * Defined in domain, implemented in infrastructure.
 */
public interface PageGraphRepository {

    /**
     * Build the page graph from all crawled pages.
     */
    PageGraph buildGraph();

    /**
     * Save PageRank scores for all pages.
     */
    void savePageRankScores(Map<String, Double> scores);

    /**
     * Get PageRank score for a specific URL.
     */
    Optional<Double> getPageRankScore(String url);

    /**
     * Get top N pages by PageRank.
     */
    Map<String, Double> getTopPages(int limit);
}
