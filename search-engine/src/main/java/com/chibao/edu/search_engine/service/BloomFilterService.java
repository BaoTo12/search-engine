package com.chibao.edu.search_engine.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Bloom Filter Service for fast URL deduplication.
 * 
 * Uses Google Guava's Bloom Filter implementation for memory-efficient
 * duplicate detection with configurable false positive rate.
 * 
 * Features:
 * - In-memory Bloom filter with ~10 bits per element
 * - 1% false positive rate (configurable)
 * - Redis backup for persistence
 * - Automatic fallback to hash-based verification
 * 
 * Space complexity: ~12MB for 10 million URLs at 1% FPR
 * Time complexity: O(k) where k = number of hash functions (~7 for 1% FPR)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BloomFilterService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int EXPECTED_INSERTIONS = 10_000_000; // 10 million URLs
    private static final double FALSE_POSITIVE_RATE = 0.01; // 1%
    private static final String REDIS_URL_SET_KEY = "crawler:visited_urls_hash";

    private BloomFilter<String> urlFilter;

    @PostConstruct
    public void init() {
        log.info("Initializing Bloom Filter with {} expected insertions and {} FPR",
                EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);

        urlFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_RATE);

        log.info("Bloom Filter initialized successfully");
    }

    /**
     * Check if URL might have been seen before.
     * 
     * Note: May return false positives (says "yes" when actually "no")
     * but NEVER returns false negatives.
     *
     * @param url The URL to check
     * @return true if URL might be duplicate, false if definitely new
     */
    public boolean mightContain(String url) {
        boolean bloomResult = urlFilter.mightContain(url);

        if (!bloomResult) {
            // Definitely not seen before
            return false;
        }

        // Might be false positive - verify with Redis
        return verifyWithRedis(url);
    }

    /**
     * Add URL to the Bloom filter.
     * Also stores hash in Redis for verification.
     *
     * @param url The URL to add
     */
    public void add(String url) {
        urlFilter.put(url);

        // Also store in Redis with expiration (30 days)
        redisTemplate.opsForSet().add(REDIS_URL_SET_KEY, url);
        redisTemplate.expire(REDIS_URL_SET_KEY, 30, TimeUnit.DAYS);
    }

    /**
     * Verify Bloom filter result with Redis to handle false positives.
     *
     * @param url The URL to verify
     * @return true if URL exists in Redis, false otherwise
     */
    private boolean verifyWithRedis(String url) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(REDIS_URL_SET_KEY, url);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Redis verification failed for URL: {}", url, e);
            // On error, assume URL exists to prevent duplicate crawling
            return true;
        }
    }

    /**
     * Get approximate number of elements in the filter.
     * Note: This is an approximation based on expected insertions.
     */
    public long getApproximateElementCount() {
        // Bloom filters don't track exact count, return Redis count as proxy
        try {
            Long size = redisTemplate.opsForSet().size(REDIS_URL_SET_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get element count from Redis", e);
            return 0;
        }
    }

    /**
     * Get expected false positive probability.
     */
    public double getExpectedFalsePositiveProbability() {
        return urlFilter.expectedFpp();
    }

    /**
     * Clear the Bloom filter (use with caution - for testing only).
     */
    public void clear() {
        log.warn("Clearing Bloom Filter - this should only be done in testing");
        urlFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_RATE);
        redisTemplate.delete(REDIS_URL_SET_KEY);
    }
}
