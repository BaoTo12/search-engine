package com.chibao.edu.search_engine.domain.resilience.service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Retry strategy with exponential backoff.
 * 
 * Features:
 * - Configurable max attempts
 * - Exponential backoff with jitter
 * - Specific exception handling
 * 
 * NO framework dependencies - pure domain logic.
 */
public class RetryStrategyService {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(100);
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(10);

    /**
     * Execute operation with exponential backoff retry.
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(operation, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Execute operation with custom max attempts.
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxAttempts) {
        return executeWithRetry(
                operation,
                maxAttempts,
                DEFAULT_INITIAL_DELAY,
                DEFAULT_MULTIPLIER,
                DEFAULT_MAX_DELAY);
    }

    /**
     * Execute operation with full configuration.
     */
    public <T> T executeWithRetry(
            Supplier<T> operation,
            int maxAttempts,
            Duration initialDelay,
            double multiplier,
            Duration maxDelay) {

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt == maxAttempts) {
                    break; // Don't sleep on last attempt
                }

                // Calculate delay with exponential backoff
                long delayMillis = calculateDelay(attempt, initialDelay, multiplier, maxDelay);

                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryException("Retry interrupted", ie);
                }
            }
        }

        throw new RetryException(
                String.format("Operation failed after %d attempts", maxAttempts),
                lastException);
    }

    /**
     * Calculate delay with exponential backoff and jitter.
     */
    private long calculateDelay(int attempt, Duration initialDelay, double multiplier, Duration maxDelay) {
        // Exponential backoff: delay = initialDelay * (multiplier ^ (attempt - 1))
        long delay = (long) (initialDelay.toMillis() * Math.pow(multiplier, attempt - 1));

        // Apply max delay cap
        delay = Math.min(delay, maxDelay.toMillis());

        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5); // Range: 0.75 to 1.25
        delay = (long) (delay * jitter);

        return delay;
    }

    /**
     * Check if exception is retryable.
     */
    public boolean isRetryable(Exception e) {
        // Retry on network errors, timeouts, service unavailable
        return e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException
                || e instanceof java.io.IOException
                || (e.getMessage() != null && e.getMessage().contains("503"))
                || (e.getMessage() != null && e.getMessage().contains("timeout"));
    }

    /**
     * Execute with conditional retry (only retry specific exceptions).
     */
    public <T> T executeWithConditionalRetry(Supplier<T> operation, int maxAttempts) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!isRetryable(e)) {
                    // Don't retry non-retryable exceptions
                    throw new RetryException("Non-retryable exception", e);
                }

                lastException = e;

                if (attempt < maxAttempts) {
                    long delay = calculateDelay(attempt, DEFAULT_INITIAL_DELAY, DEFAULT_MULTIPLIER, DEFAULT_MAX_DELAY);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RetryException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RetryException(
                String.format("Operation failed after %d attempts", maxAttempts),
                lastException);
    }

    public static class RetryException extends RuntimeException {
        public RetryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
