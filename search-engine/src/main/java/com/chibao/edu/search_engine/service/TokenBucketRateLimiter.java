package com.chibao.edu.search_engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Token Bucket Rate Limiter for per-domain politeness.
 * 
 * Implements the token bucket algorithm using Redis and Lua scripts
 * for atomic operations. This ensures fair rate limiting across
 * distributed crawler instances.
 * 
 * Algorithm:
 * - Each domain has a bucket with max capacity (e.g., 10 requests)
 * - Bucket refills at a constant rate (e.g., 1 token/second)
 * - Requests consume tokens; if bucket is empty, request is denied
 * - Allows bursts while maintaining average rate limit
 * 
 * Pros:
 * - Flexible: allows controlled bursts
 * - Fair: enforces average rate limit
 * - Distributed: works across multiple crawler instances
 * 
 * Use case: Respectful crawling without overloading target servers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    // Default configuration (can be made configurable per domain)
    private static final int DEFAULT_CAPACITY = 10; // Max burst size
    private static final double DEFAULT_REFILL_RATE = 1.0; // Tokens per second
    private static final String KEY_PREFIX = "rate_limit:token_bucket:";

    /**
     * Lua script for atomic token bucket operations.
     * This ensures thread-safety and correctness in distributed environment.
     */
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])

            -- Get current bucket state
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            -- Initialize if bucket doesn't exist
            if tokens == nil then
                tokens = capacity
                last_refill = now
            end

            -- Calculate tokens to add based on elapsed time
            local elapsed = math.max(0, now - last_refill)
            local tokens_to_add = elapsed * refill_rate
            tokens = math.min(capacity, tokens + tokens_to_add)

            -- Try to consume requested tokens
            if tokens >= requested then
                tokens = tokens - requested
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                redis.call('EXPIRE', key, 3600) -- 1 hour TTL
                return 1 -- Success
            else
                -- Not enough tokens, update state anyway for accuracy
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
                redis.call('EXPIRE', key, 3600)
                return 0 -- Denied
            end
            """;

    /**
     * Try to acquire permission to crawl a domain.
     *
     * @param domain The domain to check
     * @return true if allowed, false if rate limited
     */
    public boolean tryAcquire(String domain) {
        return tryAcquire(domain, 1, DEFAULT_CAPACITY, DEFAULT_REFILL_RATE);
    }

    /**
     * Try to acquire tokens for a domain with custom configuration.
     *
     * @param domain          The domain to check
     * @param tokensRequested Number of tokens to request (usually 1)
     * @param capacity        Bucket capacity
     * @param refillRate      Tokens per second
     * @return true if allowed, false if rate limited
     */
    public boolean tryAcquire(String domain, int tokensRequested, int capacity, double refillRate) {
        String key = KEY_PREFIX + domain;
        long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(LUA_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Arrays.asList(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(now),
                    String.valueOf(tokensRequested));

            boolean allowed = result != null && result == 1;

            if (!allowed) {
                log.debug("Rate limit exceeded for domain: {}", domain);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error executing rate limit check for domain: {}", domain, e);
            // On error, deny request to prevent overloading
            return false;
        }
    }

    /**
     * Get current token count for a domain (for monitoring).
     *
     * @param domain The domain to check
     * @return Current number of tokens available
     */
    public double getCurrentTokens(String domain) {
        String key = KEY_PREFIX + domain;
        try {
            String tokensStr = (String) redisTemplate.opsForHash().get(key, "tokens");
            return tokensStr != null ? Double.parseDouble(tokensStr) : DEFAULT_CAPACITY;
        } catch (Exception e) {
            log.error("Error getting token count for domain: {}", domain, e);
            return 0.0;
        }
    }

    /**
     * Reset rate limit for a domain (for testing or manual intervention).
     *
     * @param domain The domain to reset
     */
    public void reset(String domain) {
        String key = KEY_PREFIX + domain;
        redisTemplate.delete(key);
        log.info("Reset rate limit for domain: {}", domain);
    }

    /**
     * Calculate wait time until next token is available.
     *
     * @param domain The domain to check
     * @return Wait time in milliseconds, or 0 if tokens available
     */
    public long getWaitTimeMs(String domain) {
        String key = KEY_PREFIX + domain;
        try {
            String tokensStr = (String) redisTemplate.opsForHash().get(key, "tokens");
            if (tokensStr == null) {
                return 0; // Bucket not initialized, no wait needed
            }

            double tokens = Double.parseDouble(tokensStr);
            if (tokens >= 1.0) {
                return 0; // Tokens available
            }

            // Calculate time needed to refill to 1 token
            double tokensNeeded = 1.0 - tokens;
            long waitMs = (long) ((tokensNeeded / DEFAULT_REFILL_RATE) * 1000);
            return waitMs;

        } catch (Exception e) {
            log.error("Error calculating wait time for domain: {}", domain, e);
            return 1000; // Default 1 second wait on error
        }
    }
}
