package com.chibao.edu.search_engine.domain.deduplication.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;

/**
 * Bloom Filter service for fast duplicate URL detection.
 * Provides O(1) probabilistic duplicate checking.
 * 
 * Trade-off: Small false positive rate, but NO false negatives.
 * If it says "might be duplicate" -> check database
 * If it says "definitely new" -> it's new!
 */
public class BloomFilterService {

    private final BloomFilter<CharSequence> urlBloomFilter;
    private static final int EXPECTED_INSERTIONS = 10_000_000; // 10 million URLs
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01; // 1% false positive rate

    public BloomFilterService() {
        this.urlBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY);
    }

    /**
     * Constructor with custom parameters.
     */
    public BloomFilterService(int expectedInsertions, double falsePositiveRate) {
        this.urlBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                falsePositiveRate);
    }

    /**
     * Add URL to the bloom filter.
     */
    public void add(String url) {
        urlBloomFilter.put(url);
    }

    /**
     * Check if URL might be a duplicate.
     * - Returns true: MIGHT be duplicate (need to check database)
     * - Returns false: DEFINITELY NOT duplicate (safe to skip DB check)
     */
    public boolean mightContain(String url) {
        return urlBloomFilter.mightContain(url);
    }

    /**
     * Get approximate number of URLs in the filter.
     */
    public long approximateElementCount() {
        return urlBloomFilter.approximateElementCount();
    }

    /**
     * Get current false positive probability.
     */
    public double expectedFpp() {
        return urlBloomFilter.expectedFpp();
    }

    /**
     * Clear all entries (use with caution!).
     */
    public BloomFilter<CharSequence> getFilter() {
        return urlBloomFilter;
    }
}
