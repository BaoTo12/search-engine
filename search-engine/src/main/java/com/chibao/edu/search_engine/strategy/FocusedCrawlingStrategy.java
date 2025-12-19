package com.chibao.edu.search_engine.strategy;

import org.springframework.stereotype.Component;

/**
 * Focused Crawling strategy for topic-specific indexing.
 * 
 * Targets specific topics or domains by:
 * - Analyzing URL patterns
 * - Checking domain relevance
 * - Considering change frequency
 * - Boosting known high-quality domains
 * 
 * This is a simplified implementation. A full implementation would use:
 * - ML classifiers to predict page relevance
 * - Topic modeling
 * - Historical hit rate tracking
 * 
 * Pros:
 * - Very efficient for topic-specific crawling
 * - Minimizes storage of irrelevant content
 * - Good for vertical search engines
 * 
 * Cons:
 * - May miss related content
 * - Requires topic definition/training
 * 
 * Use case: Specialized search engines (e.g., academic papers, code, news)
 */
@Component("focusedStrategy")
public class FocusedCrawlingStrategy implements URLPrioritizationStrategy {

    // In a real implementation, these would be loaded from configuration
    private static final String[] HIGH_VALUE_DOMAINS = {
            "wikipedia.org", "github.com", "stackoverflow.com",
            "medium.com", "dev.to", "arxiv.org"
    };

    @Override
    public double calculatePriority(
            String url,
            int depth,
            double pageRank,
            double domainAuthority,
            double changeFrequency) {
        // Base score from quality signals
        double baseScore = (pageRank * 0.5) + (domainAuthority * 0.3);

        // Boost for high-value domains (topic-specific)
        double domainBoost = 1.0;
        for (String domain : HIGH_VALUE_DOMAINS) {
            if (url.contains(domain)) {
                domainBoost = 2.0;
                break;
            }
        }

        // Boost for frequently changing content (news, blogs)
        double freshnessBoost = 1.0 + (changeFrequency / 20.0);

        // URL pattern analysis (simple heuristics)
        double patternScore = analyzeURLPattern(url);

        // Combine all factors
        double priority = baseScore * domainBoost * freshnessBoost * patternScore;

        // Light depth penalty (we still want to explore)
        priority = priority / Math.sqrt(depth + 1.0);

        return priority * 1000.0;
    }

    /**
     * Analyze URL patterns to determine relevance.
     * This is a simplified implementation - real version would use ML.
     */
    private double analyzeURLPattern(String url) {
        double score = 1.0;

        // Prefer articles, docs, tutorials
        if (url.contains("/article/") || url.contains("/post/") ||
                url.contains("/tutorial/") || url.contains("/doc/")) {
            score += 0.5;
        }

        // Penalize very deep paths (likely less important)
        int pathDepth = url.split("/").length;
        if (pathDepth > 6) {
            score *= 0.8;
        }

        // Penalize query parameters (often dynamic/duplicate content)
        if (url.contains("?") && url.contains("&")) {
            score *= 0.9;
        }

        return score;
    }

    @Override
    public String getStrategyName() {
        return "Focused Crawling (Topic-Specific)";
    }
}
