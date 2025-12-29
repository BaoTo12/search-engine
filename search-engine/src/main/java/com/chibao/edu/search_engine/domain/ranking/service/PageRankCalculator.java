package com.chibao.edu.search_engine.domain.ranking.service;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for calculating PageRank scores.
 * Implements the PageRank algorithm: PR(A) = (1-d) + d × Σ(PR(Ti) / C(Ti))
 * 
 * This is a DOMAIN SERVICE - no framework dependencies!
 */
public class PageRankCalculator {

    private static final double DAMPING_FACTOR = 0.85;
    private static final double EPSILON = 0.0001; // Convergence threshold
    private static final int MAX_ITERATIONS = 100;

    /**
     * Calculate PageRank scores for all pages in the graph.
     * Uses power iteration method until convergence.
     */
    public Map<String, Double> calculate(PageGraph graph) {
        if (graph == null || graph.getPages().isEmpty()) {
            return Map.of();
        }

        int numPages = graph.getPages().size();
        Map<String, Double> pageRank = new HashMap<>();
        Map<String, Double> newPageRank = new HashMap<>();

        // Initialize all pages with equal PageRank
        double initialRank = 1.0 / numPages;
        for (String pageUrl : graph.getPages()) {
            pageRank.put(pageUrl, initialRank);
        }

        // Power iteration
        int iteration = 0;
        boolean converged = false;

        while (!converged && iteration < MAX_ITERATIONS) {
            iteration++;

            // Calculate new PageRank for each page
            for (String page : graph.getPages()) {
                double rank = calculatePageRank(page, graph, pageRank);
                newPageRank.put(page, rank);
            }

            // Check convergence
            converged = hasConverged(pageRank, newPageRank);

            // Swap for next iteration
            pageRank = new HashMap<>(newPageRank);
            newPageRank.clear();
        }

        return pageRank;
    }

    /**
     * Calculate PageRank for a single page.
     * PR(A) = (1-d) + d × Σ(PR(Ti) / C(Ti))
     */
    private double calculatePageRank(String page, PageGraph graph, Map<String, Double> currentRanks) {
        // Get incoming links to this page
        var inboundLinks = graph.getInboundLinks(page);

        if (inboundLinks.isEmpty()) {
            // Page has no incoming links - use base rank
            return (1.0 - DAMPING_FACTOR);
        }

        // Sum contributions from incoming links
        double sum = 0.0;
        for (String incomingPage : inboundLinks) {
            double incomingRank = currentRanks.getOrDefault(incomingPage, 0.0);
            int outboundCount = graph.getOutboundLinks(incomingPage).size();

            if (outboundCount > 0) {
                sum += incomingRank / outboundCount;
            }
        }

        return (1.0 - DAMPING_FACTOR) + (DAMPING_FACTOR * sum);
    }

    /**
     * Check if PageRank scores have converged.
     */
    private boolean hasConverged(Map<String, Double> oldRanks, Map<String, Double> newRanks) {
        double totalDifference = 0.0;

        for (String page : oldRanks.keySet()) {
            double oldRank = oldRanks.get(page);
            double newRank = newRanks.getOrDefault(page, 0.0);
            totalDifference += Math.abs(newRank - oldRank);
        }

        return totalDifference < EPSILON;
    }

    /**
     * Normalize PageRank scores to sum to 1.0.
     */
    public Map<String, Double> normalize(Map<String, Double> pageRanks) {
        double total = pageRanks.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (total == 0) {
            return pageRanks;
        }

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / total);
        }

        return normalized;
    }

    /**
     * Get top N pages by PageRank score.
     */
    public java.util.List<Map.Entry<String, Double>> getTopPages(Map<String, Double> pageRanks, int n) {
        return pageRanks.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(n)
                .collect(java.util.stream.Collectors.toList());
    }
}
