package com.chibao.edu.search_engine.components;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Senior Level: Circuit Breaker Pattern Implementation
 *
 * Prevents cascading failures by:
 * - Monitoring failure rates per domain
 * - Opening circuit when threshold exceeded
 * - Providing fast-fail responses during open state
 * - Allowing periodic retry attempts (half-open state)
 * - Automatically recovering when service stabilizes
 *
 * States:
 * - CLOSED: Normal operation, requests allowed
 * - OPEN: Too many failures, requests blocked
 * - HALF_OPEN: Testing if service recovered
 *
 * Use case: Prevent crawling domains that are down/blocking us
 */
@Slf4j
@Component
public class CircuitBreakerRegistry {

    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    // Default configuration
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final int DEFAULT_SUCCESS_THRESHOLD = 2;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration DEFAULT_HALF_OPEN_TIMEOUT = Duration.ofSeconds(30);

    public CircuitBreakerRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get or create circuit breaker for domain
     */
    public CircuitBreaker getCircuitBreaker(String domain) {
        return breakers.computeIfAbsent(domain, d -> new CircuitBreaker(
                d,
                DEFAULT_FAILURE_THRESHOLD,
                DEFAULT_SUCCESS_THRESHOLD,
                DEFAULT_TIMEOUT,
                DEFAULT_HALF_OPEN_TIMEOUT,
                meterRegistry
        ));
    }

    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T execute(String domain, Supplier<T> operation) {
        CircuitBreaker breaker = getCircuitBreaker(domain);
        return breaker.execute(operation);
    }

    /**
     * Reset circuit breaker for domain
     */
    public void reset(String domain) {
        CircuitBreaker breaker = breakers.get(domain);
        if (breaker != null) {
            breaker.reset();
        }
    }

    /**
     * Get all circuit breaker states
     */
    public Map<String, CircuitBreakerState> getAllStates() {
        Map<String, CircuitBreakerState> states = new ConcurrentHashMap<>();
        breakers.forEach((domain, breaker) ->
                states.put(domain, breaker.getState()));
        return states;
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        long openCount = breakers.values().stream()
                .filter(b -> b.getState() == CircuitBreakerState.OPEN)
                .count();

        long halfOpenCount = breakers.values().stream()
                .filter(b -> b.getState() == CircuitBreakerState.HALF_OPEN)
                .count();

        long closedCount = breakers.values().stream()
                .filter(b -> b.getState() == CircuitBreakerState.CLOSED)
                .count();

        stats.put("total_breakers", breakers.size());
        stats.put("open", openCount);
        stats.put("half_open", halfOpenCount);
        stats.put("closed", closedCount);

        return stats;
    }

    public enum CircuitBreakerState {
        CLOSED,     // Normal operation
        OPEN,       // Failures exceeded threshold
        HALF_OPEN   // Testing recovery
    }

    /**
     * Circuit Breaker Implementation
     */
    @Data
    public static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final int successThreshold;
        private final Duration openTimeout;
        private final Duration halfOpenTimeout;

        private CircuitBreakerState state;
        private final AtomicInteger failureCount;
        private final AtomicInteger successCount;
        private final AtomicLong lastFailureTime;
        private final AtomicLong lastStateChange;

        // Metrics
        private final Counter successCounter;
        private final Counter failureCounter;
        private final Counter rejectedCounter;

        public CircuitBreaker(
                String name,
                int failureThreshold,
                int successThreshold,
                Duration openTimeout,
                Duration halfOpenTimeout,
                MeterRegistry meterRegistry) {

            this.name = name;
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
            this.openTimeout = openTimeout;
            this.halfOpenTimeout = halfOpenTimeout;

            this.state = CircuitBreakerState.CLOSED;
            this.failureCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
            this.lastFailureTime = new AtomicLong(0);
            this.lastStateChange = new AtomicLong(System.currentTimeMillis());

            // Initialize metrics
            this.successCounter = Counter.builder("circuit_breaker.success")
                    .tag("domain", name)
                    .description("Successful requests through circuit breaker")
                    .register(meterRegistry);

            this.failureCounter = Counter.builder("circuit_breaker.failure")
                    .tag("domain", name)
                    .description("Failed requests through circuit breaker")
                    .register(meterRegistry);

            this.rejectedCounter = Counter.builder("circuit_breaker.rejected")
                    .tag("domain", name)
                    .description("Rejected requests (circuit open)")
                    .register(meterRegistry);
        }

        /**
         * Execute operation with circuit breaker protection
         */
        public <T> T execute(Supplier<T> operation) {
            // Check if circuit should transition
            checkState();

            if (state == CircuitBreakerState.OPEN) {
                rejectedCounter.increment();
                throw new CircuitBreakerOpenException(
                        "Circuit breaker OPEN for " + name +
                                " - too many failures (threshold: " + failureThreshold + ")");
            }

            try {
                T result = operation.get();
                onSuccess();
                return result;

            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        /**
         * Record successful operation
         */
        private synchronized void onSuccess() {
            successCounter.increment();

            if (state == CircuitBreakerState.HALF_OPEN) {
                successCount.incrementAndGet();

                if (successCount.get() >= successThreshold) {
                    // Enough successes in half-open state -> close circuit
                    transitionTo(CircuitBreakerState.CLOSED);
                    failureCount.set(0);
                    successCount.set(0);
                    log.info("Circuit breaker CLOSED for {} (recovered)", name);
                }
            } else if (state == CircuitBreakerState.CLOSED) {
                // Reset failure count on success
                failureCount.set(0);
            }
        }

        /**
         * Record failed operation
         */
        private synchronized void onFailure() {
            failureCounter.increment();
            lastFailureTime.set(System.currentTimeMillis());

            if (state == CircuitBreakerState.HALF_OPEN) {
                // Failure in half-open -> back to open
                transitionTo(CircuitBreakerState.OPEN);
                successCount.set(0);
                log.warn("Circuit breaker re-OPENED for {} (failed during recovery)", name);

            } else if (state == CircuitBreakerState.CLOSED) {
                int failures = failureCount.incrementAndGet();

                if (failures >= failureThreshold) {
                    // Threshold exceeded -> open circuit
                    transitionTo(CircuitBreakerState.OPEN);
                    log.warn("Circuit breaker OPENED for {} (failures: {})", name, failures);
                }
            }
        }

        /**
         * Check if circuit state should transition
         */
        private synchronized void checkState() {
            long now = System.currentTimeMillis();
            long timeSinceStateChange = now - lastStateChange.get();

            if (state == CircuitBreakerState.OPEN) {
                // Check if we should try recovery (transition to half-open)
                if (timeSinceStateChange >= openTimeout.toMillis()) {
                    transitionTo(CircuitBreakerState.HALF_OPEN);
                    successCount.set(0);
                    log.info("Circuit breaker transitioned to HALF_OPEN for {}", name);
                }
            } else if (state == CircuitBreakerState.HALF_OPEN) {
                // Timeout in half-open -> back to open
                if (timeSinceStateChange >= halfOpenTimeout.toMillis()) {
                    transitionTo(CircuitBreakerState.OPEN);
                    log.warn("Circuit breaker timeout in HALF_OPEN for {}, reopening", name);
                }
            }
        }

        /**
         * Transition to new state
         */
        private void transitionTo(CircuitBreakerState newState) {
            this.state = newState;
            this.lastStateChange.set(System.currentTimeMillis());
        }

        /**
         * Reset circuit breaker
         */
        public synchronized void reset() {
            state = CircuitBreakerState.CLOSED;
            failureCount.set(0);
            successCount.set(0);
            lastStateChange.set(System.currentTimeMillis());
            log.info("Circuit breaker manually reset for {}", name);
        }

        /**
         * Get human-readable status
         */
        public String getStatus() {
            return String.format(
                    "State: %s, Failures: %d/%d, Successes: %d/%d, Time in state: %ds",
                    state,
                    failureCount.get(),
                    failureThreshold,
                    successCount.get(),
                    successThreshold,
                    (System.currentTimeMillis() - lastStateChange.get()) / 1000
            );
        }
    }

    /**
     * Exception thrown when circuit is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}


