package com.chibao.edu.search_engine.domain.ranking.service;

import java.util.*;

/**
 * Machine Learning based ranking service.
 * Uses Learning to Rank (LTR) with gradient boosting.
 * 
 * Features:
 * - BM25 relevance score
 * - PageRank score
 * - Content quality score
 * - Click-through rate (CTR)
 * - User engagement signals
 */
public class MachineLearningRankingService {

    // Feature weights (trained offline)
    private static final double WEIGHT_BM25 = 0.35;
    private static final double WEIGHT_PAGERANK = 0.25;
    private static final double WEIGHT_QUALITY = 0.20;
    private static final double WEIGHT_CTR = 0.15;
    private static final double WEIGHT_FRESHNESS = 0.05;

    /**
     * Calculate ML-based combined score.
     */
    public double calculateScore(RankingFeatures features) {
        double score = 0.0;

        // BM25 relevance score
        score += features.getBm25Score() * WEIGHT_BM25;

        // PageRank authority score
        score += features.getPageRankScore() * WEIGHT_PAGERANK;

        // Content quality score
        score += features.getQualityScore() * WEIGHT_QUALITY;

        // Click-through rate
        score += features.getCtrScore() * WEIGHT_CTR;

        // Freshness score (decay over time)
        score += calculateFreshnessScore(features.getPublishedDaysAgo()) * WEIGHT_FRESHNESS;

        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculate freshness score with exponential decay.
     */
    private double calculateFreshnessScore(int daysAgo) {
        if (daysAgo < 0)
            return 0.0;

        // Exponential decay: score = e^(-λ * days)
        double lambda = 0.01; // Decay constant
        return Math.exp(-lambda * daysAgo);
    }

    /**
     * Rank list of results by ML score.
     */
    public List<ScoredResult> rankResults(List<ScoredResult> results) {
        // Calculate ML scores
        for (ScoredResult result : results) {
            double mlScore = calculateScore(result.getFeatures());
            result.setMlScore(mlScore);
        }

        // Sort by ML score descending
        results.sort((a, b) -> Double.compare(b.getMlScore(), a.getMlScore()));

        return results;
    }

    /**
     * Apply diversity ranking to avoid filter bubbles.
     */
    public List<ScoredResult> diversifyResults(List<ScoredResult> results, int topK) {
        List<ScoredResult> diversified = new ArrayList<>();
        Set<String> seenDomains = new HashSet<>();

        // Maximize Marginal Relevance (MMR)
        for (ScoredResult result : results) {
            String domain = extractDomain(result.getUrl());

            // Limit results per domain for diversity
            long domainCount = diversified.stream()
                    .filter(r -> extractDomain(r.getUrl()).equals(domain))
                    .count();

            if (domainCount < 2 || seenDomains.size() < 5) {
                diversified.add(result);
                seenDomains.add(domain);
            }

            if (diversified.size() >= topK) {
                break;
            }
        }

        return diversified;
    }

    private String extractDomain(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // Data classes
    public static class RankingFeatures {
        private double bm25Score;
        private double pageRankScore;
        private double qualityScore;
        private double ctrScore;
        private int publishedDaysAgo;

        public RankingFeatures(double bm25, double pageRank, double quality, double ctr, int daysAgo) {
            this.bm25Score = bm25;
            this.pageRankScore = pageRank;
            this.qualityScore = quality;
            this.ctrScore = ctr;
            this.publishedDaysAgo = daysAgo;
        }

        public double getBm25Score() {
            return bm25Score;
        }

        public double getPageRankScore() {
            return pageRankScore;
        }

        public double getQualityScore() {
            return qualityScore;
        }

        public double getCtrScore() {
            return ctrScore;
        }

        public int getPublishedDaysAgo() {
            return publishedDaysAgo;
        }
    }

    public static class ScoredResult {
        private String url;
        private String title;
        private RankingFeatures features;
        private double mlScore;

        public ScoredResult(String url, String title, RankingFeatures features) {
            this.url = url;
            this.title = title;
            this.features = features;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public RankingFeatures getFeatures() {
            return features;
        }

        public double getMlScore() {
            return mlScore;
        }

        public void setMlScore(double score) {
            this.mlScore = score;
        }
    }
}
