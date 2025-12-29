package com.chibao.edu.search_engine.domain.crawling.service;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for URL prioritization.
 * Calculates priority scores for URLs based on multiple strategies.
 * 
 * NO framework dependencies - pure domain logic.
 */
public class UrlPrioritizationService {

    private static final Map<String, Double> DOMAIN_TRUST_SCORES = new HashMap<>();

    static {
        // High trust domains
        DOMAIN_TRUST_SCORES.put("wikipedia.org", 0.9);
        DOMAIN_TRUST_SCORES.put("github.com", 0.85);
        DOMAIN_TRUST_SCORES.put("stackoverflow.com", 0.85);
        DOMAIN_TRUST_SCORES.put("medium.com", 0.75);

        // Default for unknown domains
        DOMAIN_TRUST_SCORES.put("default", 0.5);
    }

    /**
     * Calculate priority score for a URL (0-1 scale, higher is better).
     */
    public double calculatePriority(CrawlJob job) {
        double score = 0.0;

        // Factor 1: Domain trust (40%)
        score += getDomainTrustScore(job.getUrl().getValue()) * 0.4;

        // Factor 2: Depth penalty (30%)
        score += getDepthScore(job.getDepth().getValue()) * 0.3;

        // Factor 3: URL characteristics (30%)
        score += getUrlCharacteristicScore(job.getUrl().getValue()) * 0.3;

        return Math.min(1.0, Math.max(0.0, score));
    }

    /**
     * Get domain trust score based on known high-quality domains.
     */
    private double getDomainTrustScore(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null) {
                return 0.5;
            }

            // Check for exact match
            for (Map.Entry<String, Double> entry : DOMAIN_TRUST_SCORES.entrySet()) {
                if (host.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            // Special handling for .edu and .gov
            if (host.endsWith(".edu")) {
                return 0.85;
            }
            if (host.endsWith(".gov")) {
                return 0.80;
            }

            return DOMAIN_TRUST_SCORES.get("default");

        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * Calculate score based on crawl depth (prefer shallower pages).
     */
    private double getDepthScore(int depth) {
        // Exponential decay: depth 0 = 1.0, depth 1 = 0.7, depth 2 = 0.5, depth 3 = 0.3
        return Math.max(0.1, 1.0 - (depth * 0.3));
    }

    /**
     * Score based on URL characteristics (length, special chars, etc.).
     */
    private double getUrlCharacteristicScore(String url) {
        double score = 1.0;

        // Penalize very long URLs
        if (url.length() > 200) {
            score -= 0.3;
        } else if (url.length() > 100) {
            score -= 0.15;
        }

        // Penalize URLs with many query parameters
        long queryParamCount = url.chars().filter(ch -> ch == '&').count();
        if (queryParamCount > 5) {
            score -= 0.2;
        } else if (queryParamCount > 3) {
            score -= 0.1;
        }

        // Penalize URLs with session IDs
        if (url.contains("sessionid") || url.contains("PHPSESSID") || url.contains("jsessionid")) {
            score -= 0.3;
        }

        // Bonus for likely content pages
        if (url.contains("/blog/") || url.contains("/article/") || url.contains("/post/")) {
            score += 0.2;
        }

        // Penalize likely non-content pages
        if (url.contains("/admin/") || url.contains("/login") || url.contains("/api/")) {
            score -= 0.5;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Determine if URL should be immediately crawled (high priority).
     */
    public boolean isHighPriority(CrawlJob job) {
        double priority = calculatePriority(job);
        return priority > 0.7;
    }

    /**
     * Determine if URL should be skipped (very low priority).
     */
    public boolean shouldSkip(CrawlJob job) {
        double priority = calculatePriority(job);
        return priority < 0.2;
    }
}
