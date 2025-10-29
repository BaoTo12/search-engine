package com.chibao.edu.search_engine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

// ========================================
// BULKHEAD PATTERN (Resource Isolation)
// ========================================

/**
 * Senior Level: Bulkhead Pattern
 *
 * Isolates resources to prevent one domain/failure from consuming all threads
 * Uses separate thread pools per domain category
 */

@Slf4j
@Component
public class BulkheadService {

    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    private static final int DEFAULT_MAX_CONCURRENT = 10;

    /**
     * Execute with bulkhead protection (limited concurrency)
     */
    public <T> T execute(String domain, int maxConcurrent, Supplier<T> operation) {
        java.util.concurrent.Semaphore semaphore = semaphores.computeIfAbsent(
                domain,
                d -> new java.util.concurrent.Semaphore(maxConcurrent)
        );

        try {
            // Try to acquire permit (non-blocking)
            if (!semaphore.tryAcquire(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new BulkheadFullException(
                        "Bulkhead full for domain: " + domain +
                                " (max concurrent: " + maxConcurrent + ")");
            }

            try {
                return operation.get();
            } finally {
                semaphore.release();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for bulkhead", e);
        }
    }

    /**
     * Get current concurrent executions for domain
     */
    public int getCurrentConcurrency(String domain) {
        java.util.concurrent.Semaphore semaphore = semaphores.get(domain);
        if (semaphore == null) {
            return 0;
        }
        return DEFAULT_MAX_CONCURRENT - semaphore.availablePermits();
    }

    /**
     * Get bulkhead statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        semaphores.forEach((domain, semaphore) -> {
            Map<String, Integer> domainStats = new ConcurrentHashMap<>();
            domainStats.put("max_concurrent", DEFAULT_MAX_CONCURRENT);
            domainStats.put("available", semaphore.availablePermits());
            domainStats.put("in_use", DEFAULT_MAX_CONCURRENT - semaphore.availablePermits());
            stats.put(domain, domainStats);
        });

        return stats;
    }

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) {
            super(message);
        }
    }
}