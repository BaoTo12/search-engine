package com.chibao.edu.search_engine.domain.resilience.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circuit Breaker pattern implementation for fault tolerance.
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failure threshold exceeded, requests fail fast
 * - HALF_OPEN: Testing if service recovered
 * 
 * NO framework dependencies - pure domain logic.
 */
public class CircuitBreakerService {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_SUCCESS_THRESHOLD = 2;

    /**
     * Get or create circuit breaker for a resource.
     */
    public CircuitBreaker getCircuitBreaker(String resourceName) {
        return circuitBreakers.computeIfAbsent(
                resourceName,
                k -> new CircuitBreaker(resourceName, DEFAULT_FAILURE_THRESHOLD, DEFAULT_TIMEOUT,
                        DEFAULT_SUCCESS_THRESHOLD));
    }

    /**
     * Execute operation with circuit breaker protection.
     */
    public <T> T execute(String resourceName, Operation<T> operation) throws Exception {
        CircuitBreaker breaker = getCircuitBreaker(resourceName);

        if (!breaker.allowRequest()) {
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for: " + resourceName);
        }

        try {
            T result = operation.execute();
            breaker.recordSuccess();
            return result;
        } catch (Exception e) {
            breaker.recordFailure();
            throw e;
        }
    }

    /**
     * Check if circuit breaker is open for a resource.
     */
    public boolean isOpen(String resourceName) {
        CircuitBreaker breaker = circuitBreakers.get(resourceName);
        return breaker != null && breaker.getState() == CircuitBreakerState.OPEN;
    }

    /**
     * Reset circuit breaker for a resource.
     */
    public void reset(String resourceName) {
        CircuitBreaker breaker = circuitBreakers.get(resourceName);
        if (breaker != null) {
            breaker.reset();
        }
    }

    /**
     * Get all circuit breaker states.
     */
    public Map<String, CircuitBreakerState> getAllStates() {
        Map<String, CircuitBreakerState> states = new ConcurrentHashMap<>();
        circuitBreakers.forEach((name, breaker) -> states.put(name, breaker.getState()));
        return states;
    }

    // Inner class representing a single circuit breaker
    public static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final Duration timeout;
        private final int successThreshold;

        private CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private int failureCount = 0;
        private int successCount = 0;
        private Instant lastFailureTime;

        public CircuitBreaker(String name, int failureThreshold, Duration timeout, int successThreshold) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.successThreshold = successThreshold;
        }

        public synchronized boolean allowRequest() {
            if (state == CircuitBreakerState.CLOSED) {
                return true;
            }

            if (state == CircuitBreakerState.OPEN) {
                // Check if timeout has elapsed
                if (lastFailureTime != null &&
                        Duration.between(lastFailureTime, Instant.now()).compareTo(timeout) > 0) {
                    // Transition to HALF_OPEN
                    state = CircuitBreakerState.HALF_OPEN;
                    successCount = 0;
                    return true;
                }
                return false;
            }

            // HALF_OPEN state
            return true;
        }

        public synchronized void recordSuccess() {
            if (state == CircuitBreakerState.HALF_OPEN) {
                successCount++;
                if (successCount >= successThreshold) {
                    // Transition to CLOSED
                    state = CircuitBreakerState.CLOSED;
                    failureCount = 0;
                    successCount = 0;
                }
            } else if (state == CircuitBreakerState.CLOSED) {
                // Reset failure count on success
                failureCount = 0;
            }
        }

        public synchronized void recordFailure() {
            lastFailureTime = Instant.now();

            if (state == CircuitBreakerState.HALF_OPEN) {
                // Transition back to OPEN
                state = CircuitBreakerState.OPEN;
                successCount = 0;
            } else if (state == CircuitBreakerState.CLOSED) {
                failureCount++;
                if (failureCount >= failureThreshold) {
                    // Transition to OPEN
                    state = CircuitBreakerState.OPEN;
                }
            }
        }

        public synchronized void reset() {
            state = CircuitBreakerState.CLOSED;
            failureCount = 0;
            successCount = 0;
            lastFailureTime = null;
        }

        public CircuitBreakerState getState() {
            return state;
        }

        public String getName() {
            return name;
        }

        public int getFailureCount() {
            return failureCount;
        }
    }

    public enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
