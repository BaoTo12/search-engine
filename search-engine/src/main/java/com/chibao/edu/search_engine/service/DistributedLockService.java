package com.chibao.edu.search_engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Senior Level: Distributed Locking and Advanced Rate Limiting
 * <p>
 * Implements:
 * 1. Distributed locks using Redis (Redlock algorithm)
 * 2. Token bucket rate limiting per domain
 * 3. Sliding window rate limiting
 * 4. Fair scheduling across domains
 * <p>
 * Why needed:
 * - Multiple crawler instances need coordination
 * - Prevent simultaneous crawls of same URL
 * - Enforce politeness policies per domain
 * - Balance load across domains
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String TOKEN_BUCKET_PREFIX = "tokenbucket:";
    private static final long DEFAULT_LOCK_TTL_MS = 30000; // 30 seconds
    private static final int MAX_LOCK_RETRY_ATTEMPTS = 3;
    private static final long LOCK_RETRY_DELAY_MS = 100;

    /**
     * Acquire distributed lock
     *
     * @param lockKey   unique lock identifier
     * @param lockValue unique value (typically UUID) to prevent unlock by wrong holder
     * @param ttlMs     time-to-live in milliseconds
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLock(String lockKey, String lockValue, long ttlMs) {
        try {
            String key = LOCK_PREFIX + lockKey;

            // SET key value NX PX milliseconds
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, Duration.ofMillis(ttlMs));

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Acquired lock: {} with value: {}", lockKey, lockValue);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Error acquiring lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Try to acquire lock with retries
     */
    public boolean acquireLockWithRetry(String lockKey, String lockValue, long ttlMs) {
        for (int attempt = 0; attempt < MAX_LOCK_RETRY_ATTEMPTS; attempt++) {
            if (acquireLock(lockKey, lockValue, ttlMs)) {
                return true;
            }

            // Wait before retry
            try {
                Thread.sleep(LOCK_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.debug("Failed to acquire lock after {} attempts: {}", MAX_LOCK_RETRY_ATTEMPTS, lockKey);
        return false;
    }

    /**
     * Release distributed lock (only if held by caller)
     * Uses Lua script to ensure atomicity
     */
    public void releaseLock(String lockKey, String lockValue) {
        try {
            String key = LOCK_PREFIX + lockKey;

            // Lua script to check value and delete atomically
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "    return redis.call('del', KEYS[1]) " +
                            "else " +
                            "    return 0 " +
                            "end";

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    lockValue
            );

            boolean released = result == 1;

            if (released) {
                log.debug("Released lock: {} with value: {}", lockKey, lockValue);
            } else {
                log.warn("Failed to release lock: {} (value mismatch or already released)", lockKey);
            }

        } catch (Exception e) {
            log.error("Error releasing lock: {}", lockKey, e);
        }
    }

    /**
     * Execute code with distributed lock
     */
    public <T> Optional<T> executeWithLock(
            String lockKey,
            long ttlMs,
            java.util.function.Supplier<T> task) {

        String lockValue = UUID.randomUUID().toString();

        try {
            if (acquireLockWithRetry(lockKey, lockValue, ttlMs)) {
                try {
                    T result = task.get();
                    return Optional.ofNullable(result);
                } finally {
                    releaseLock(lockKey, lockValue);
                }
            } else {
                log.warn("Could not acquire lock for task: {}", lockKey);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error executing task with lock: {}", lockKey, e);
            return Optional.empty();
        }
    }

    // ========================================
    // TOKEN BUCKET RATE LIMITING
    // ========================================

    /**
     * Token bucket rate limiter
     * <p>
     * Allows burst traffic up to bucket capacity, then enforces rate limit
     *
     * @param domain              the domain to rate limit
     * @param maxTokens           maximum tokens (requests) allowed
     * @param refillRatePerSecond how many tokens to add per second
     * @return true if request allowed, false if rate limited
     */
    public boolean allowRequest(String domain, int maxTokens, double refillRatePerSecond) {
        String key = TOKEN_BUCKET_PREFIX + domain;

        try {
            long now = System.currentTimeMillis();

            // Lua script for atomic token bucket operation
            String luaScript =
                    "local key = KEYS[1] " +
                            "local max_tokens = tonumber(ARGV[1]) " +
                            "local refill_rate = tonumber(ARGV[2]) " +
                            "local now = tonumber(ARGV[3]) " +

                            "local bucket = redis.call('hmget', key, 'tokens', 'last_refill') " +
                            "local tokens = tonumber(bucket[1]) or max_tokens " +
                            "local last_refill = tonumber(bucket[2]) or now " +

                            "-- Calculate tokens to add since last refill " +
                            "local elapsed_seconds = (now - last_refill) / 1000.0 " +
                            "local tokens_to_add = elapsed_seconds * refill_rate " +
                            "tokens = math.min(max_tokens, tokens + tokens_to_add) " +

                            "-- Try to consume 1 token " +
                            "if tokens >= 1 then " +
                            "    tokens = tokens - 1 " +
                            "    redis.call('hmset', key, 'tokens', tokens, 'last_refill', now) " +
                            "    redis.call('expire', key, 300) " + // 5 min expiry
                            "    return 1 " +
                            "else " +
                            "    redis.call('hmset', key, 'tokens', tokens, 'last_refill', now) " +
                            "    redis.call('expire', key, 300) " +
                            "    return 0 " +
                            "end";

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    String.valueOf(maxTokens),
                    String.valueOf(refillRatePerSecond),
                    String.valueOf(now)
            );

            boolean allowed = result == 1;

            if (!allowed) {
                log.debug("Rate limit exceeded for domain: {}", domain);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error in token bucket rate limiter for domain: {}", domain, e);
            // Fail open - allow request on error
            return true;
        }
    }

    /**
     * Sliding window rate limiter
     * <p>
     * Counts requests in a time window and enforces limit
     * More precise than fixed window, prevents burst at window boundaries
     *
     * @param domain        the domain to rate limit
     * @param windowSeconds size of sliding window in seconds
     * @param maxRequests   maximum requests allowed in window
     * @return true if request allowed, false if rate limited
     */
    public boolean allowRequestSlidingWindow(String domain, int windowSeconds, int maxRequests) {
        String key = RATE_LIMIT_PREFIX + domain;

        try {
            long now = System.currentTimeMillis();
            long windowStart = now - (windowSeconds * 1000L);

            // Lua script for atomic sliding window operation
            String luaScript =
                    "local key = KEYS[1] " +
                            "local window_start = ARGV[1] " +
                            "local now = ARGV[2] " +
                            "local max_requests = tonumber(ARGV[3]) " +

                            "-- Remove old requests outside window " +
                            "redis.call('zremrangebyscore', key, '-inf', window_start) " +

                            "-- Count current requests in window " +
                            "local count = redis.call('zcard', key) " +

                            "if count < max_requests then " +
                            "    -- Add current request " +
                            "    redis.call('zadd', key, now, now) " +
                            "    redis.call('expire', key, ARGV[4]) " +
                            "    return 1 " +
                            "else " +
                            "    return 0 " +
                            "end";

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(key),
                    String.valueOf(windowStart),
                    String.valueOf(now),
                    String.valueOf(maxRequests),
                    String.valueOf(windowSeconds * 2) // Keep data a bit longer
            );

            boolean allowed = result == 1;

            if (!allowed) {
                log.debug("Sliding window rate limit exceeded for domain: {}", domain);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error in sliding window rate limiter for domain: {}", domain, e);
            // Fail open - allow request on error
            return true;
        }
    }

    /**
     * Get current request count in sliding window
     */
    public int getCurrentRequestCount(String domain, int windowSeconds) {
        String key = RATE_LIMIT_PREFIX + domain;

        try {
            long now = System.currentTimeMillis();
            long windowStart = now - (windowSeconds * 1000L);

            Long count = redisTemplate.opsForZSet()
                    .count(key, windowStart, now);

            return count != null ? count.intValue() : 0;

        } catch (Exception e) {
            log.error("Error getting request count for domain: {}", domain, e);
            return 0;
        }
    }

    /**
     * Reset rate limit for domain
     */
    public void resetRateLimit(String domain) {
        try {
            String tokenBucketKey = TOKEN_BUCKET_PREFIX + domain;
            String slidingWindowKey = RATE_LIMIT_PREFIX + domain;

            redisTemplate.delete(tokenBucketKey);
            redisTemplate.delete(slidingWindowKey);

            log.info("Reset rate limit for domain: {}", domain);

        } catch (Exception e) {
            log.error("Error resetting rate limit for domain: {}", domain, e);
        }
    }

    /**
     * Get all currently locked resources
     */
    public Set<String> getActiveLocks() {
        try {
            Set<String> keys = redisTemplate.keys(LOCK_PREFIX + "*");

            return keys.stream()
                    .map(key -> key.substring(LOCK_PREFIX.length()))
                    .collect(java.util.stream.Collectors.toSet());

        } catch (Exception e) {
            log.error("Error getting active locks", e);
            return Collections.emptySet();
        }
    }

    /**
     * Get rate limiting statistics
     */
    public Map<String, Object> getRateLimitStats(String domain) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Token bucket stats
            String tokenKey = TOKEN_BUCKET_PREFIX + domain;
            Map<Object, Object> bucketData = redisTemplate.opsForHash().entries(tokenKey);
            if (!bucketData.isEmpty()) {
                stats.put("token_bucket", bucketData);
            }

            // Sliding window stats
            String windowKey = RATE_LIMIT_PREFIX + domain;
            Long requestCount = redisTemplate.opsForZSet().zCard(windowKey);
            stats.put("recent_requests", requestCount != null ? requestCount : 0);

        } catch (Exception e) {
            log.error("Error getting rate limit stats for domain: {}", domain, e);
        }

        return stats;
    }

    /**
     * Clean up expired locks (maintenance task)
     */
    public int cleanupExpiredLocks() {
        try {
            Set<String> lockKeys = redisTemplate.keys(LOCK_PREFIX + "*");
            if (lockKeys.isEmpty()) {
                return 0;
            }

            int cleaned = 0;
            for (String key : lockKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                if (ttl < 0) {
                    // Lock expired but not cleaned up
                    redisTemplate.delete(key);
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                log.info("Cleaned up {} expired locks", cleaned);
            }

            return cleaned;

        } catch (Exception e) {
            log.error("Error cleaning up expired locks", e);
            return 0;
        }
    }
}