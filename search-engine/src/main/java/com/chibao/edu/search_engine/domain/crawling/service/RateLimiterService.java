package com.chibao.edu.search_engine.domain.crawling.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter service for controlling crawl rate per domain.
 * Implements Token Bucket algorithm.
 * 
 * Features:
 * - Per-domain rate limiting
 * - Configurable politeness delay
 * - Respects robots.txt crawl-delay
 * 
 * NO framework dependencies - pure domain logic.
 */
public class RateLimiterService {

    private final Map<String, TokenBucket> domainBuckets = new ConcurrentHashMap<>();

    private static final Duration DEFAULT_POLITENESS_DELAY = Duration.ofMillis(1000); // 1 second
    private static final int DEFAULT_BURST_SIZE = 5; // Allow small bursts

    /**
     * Check if request to domain is allowed.
     */
    public boolean allowRequest(String domain) {
        return allowRequest(domain, DEFAULT_POLITENESS_DELAY);
    }

    /**
     * Check if request to domain is allowed with custom delay.
     */
    public boolean allowRequest(String domain, Duration minDelay) {
        TokenBucket bucket = domainBuckets.computeIfAbsent(
                domain,
                k -> new TokenBucket(minDelay, DEFAULT_BURST_SIZE));

        return bucket.tryAcquire();
    }

    /**
     * Wait until request is allowed (blocking).
     */
    public void waitForPermit(String domain) throws InterruptedException {
        waitForPermit(domain, DEFAULT_POLITENESS_DELAY);
    }

    /**
     * Wait until request is allowed with custom delay (blocking).
     */
    public void waitForPermit(String domain, Duration minDelay) throws InterruptedException {
        while (!allowRequest(domain, minDelay)) {
            Thread.sleep(100); // Check every 100ms
        }
    }

    /**
     * Set crawl delay for a domain (from robots.txt).
     */
    public void setCrawlDelay(String domain, Duration delay) {
        domainBuckets.put(domain, new TokenBucket(delay, DEFAULT_BURST_SIZE));
    }

    /**
     * Get time until next request is allowed.
     */
    public Duration getTimeUntilNextRequest(String domain) {
        TokenBucket bucket = domainBuckets.get(domain);
        if (bucket == null) {
            return Duration.ZERO;
        }
        return bucket.getTimeUntilNextToken();
    }

    /**
     * Reset rate limiter for a domain.
     */
    public void reset(String domain) {
        domainBuckets.remove(domain);
    }

    /**
     * Get all domains being rate limited.
     */
    public int getTrackedDomainCount() {
        return domainBuckets.size();
    }

    // Inner class implementing Token Bucket algorithm
    private static class TokenBucket {
        private final Duration refillInterval;
        private final int maxTokens;
        private int availableTokens;
        private Instant lastRefillTime;

        public TokenBucket(Duration refillInterval, int maxTokens) {
            this.refillInterval = refillInterval;
            this.maxTokens = maxTokens;
            this.availableTokens = maxTokens;
            this.lastRefillTime = Instant.now();
        }

        public synchronized boolean tryAcquire() {
            refillTokens();

            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }

            return false;
        }

        private void refillTokens() {
            Instant now = Instant.now();
            Duration elapsed = Duration.between(lastRefillTime, now);

            if (elapsed.compareTo(refillInterval) >= 0) {
                // Calculate how many tokens to add
                long intervalsElapsed = elapsed.toMillis() / refillInterval.toMillis();
                int tokensToAdd = (int) Math.min(intervalsElapsed, maxTokens - availableTokens);

                availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
                lastRefillTime = now;
            }
        }

        public Duration getTimeUntilNextToken() {
            if (availableTokens > 0) {
                return Duration.ZERO;
            }

            Instant now = Instant.now();
            Duration elapsed = Duration.between(lastRefillTime, now);

            if (elapsed.compareTo(refillInterval) >= 0) {
                return Duration.ZERO;
            }

            return refillInterval.minus(elapsed);
        }
    }
}
