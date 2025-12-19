package com.chibao.edu.search_engine.strategy;

import org.springframework.stereotype.Component;

/**
 * Best-First Search crawling strategy.
 * 
 * Prioritizes high-value pages first based on:
 * - PageRank score
 * - Domain authority
 * - Balanced with depth to ensure exploration
 * 
 * Formula: Priority = (PageRank × DomainAuthority) / (depth + 1)
 * 
 * Pros:
 * - Maximizes crawl efficiency
 * - Finds important content faster
 * - Good for limited crawl budgets
 * 
 * Cons:
 * - May miss less popular but valuable content
 * - Requires PageRank calculation
 * 
 * Use case: Focused crawling with quality emphasis
 */
@Component("bestFirstStrategy")
public class BestFirstStrategy implements URLPrioritizationStrategy {

    @Override
    public double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority,
            double changeFrequency) {
        // Combine PageRank and domain authority
        double qualityScore = (pageRank * 0.7) + (domainAuthority * 0.3);

        // Divide by (depth + 1) to balance exploration vs exploitation
        // +1 to avoid division by zero and to prevent depth 0 from having infinite
        // priority
        double priority = qualityScore / (depth + 1.0);

        // Scale up to make scores more distinguishable
        return priority * 1000.0;
    }

    @Override
    public String getStrategyName() {
        return "Best-First (Quality-Focused)";
    }
}
