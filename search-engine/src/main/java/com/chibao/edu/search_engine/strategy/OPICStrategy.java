package com.chibao.edu.search_engine.strategy;

import org.springframework.stereotype.Component;

/**
 * OPIC (Online Page Importance Computation) strategy.
 * 
 * Lightweight alternative to PageRank that doesn't require full graph analysis:
 * - Each page starts with "cash" = 1.0
 * - When crawled, splits its cash among outbound links
 * - Pages with more incoming "cash" are crawled first
 * - Approximates PageRank without global computation
 * 
 * Formula: Priority = accumulated_cash × domain_authority × freshness_factor
 * 
 * Pros:
 * - No full graph analysis needed
 * - Real-time priority calculation
 * - Scales well for large crawls
 * 
 * Cons:
 * - Less accurate than full PageRank
 * - Requires tracking cash per URL
 * 
 * Use case: Large-scale crawling with real-time prioritization
 */
@Component("opicStrategy")
public class OPICStrategy implements URLPrioritizationStrategy {

    @Override
    public double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority,
            double changeFrequency) {
        // Use PageRank as a proxy for accumulated "cash"
        // In a full implementation, this would be tracked separately
        double cash = pageRank > 0 ? pageRank : 0.1; // Minimum cash for new pages

        // Freshness factor: pages that change more frequently should be crawled sooner
        double freshnessFactor = 1.0 + (changeFrequency / 10.0);

        // Combine cash, domain authority, and freshness
        double priority = cash * domainAuthority * freshnessFactor;

        // Slight penalty for depth to encourage breadth
        priority = priority / Math.log(depth + 2.0);

        // Scale up
        return priority * 1000.0;
    }

    @Override
    public String getStrategyName() {
        return "OPIC (Online Page Importance)";
    }
}
