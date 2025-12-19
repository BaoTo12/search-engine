# Circuit Breaker Pattern

> **File:** [CircuitBreakerRegistry.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/components/CircuitBreakerRegistry.java)  
> **Pattern:** Fault tolerance with automatic recovery

---

## State Machine

```
        5 failures
CLOSED ───────────────→ OPEN
  ↑                       │
  │                       │ timeout
  │                       ↓
  └───── success ─── HALF_OPEN
```

**States:**
- **CLOSED:** Normal operation, requests allowed
- **OPEN:** Failing, block all requests (fail fast)
- **HALF_OPEN:** Testing recovery, allow limited requests

---

## Implementation

```java
@Component
public class CircuitBreakerRegistry {
    
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int FAILURE_THRESHOLD = 5;
    private static final long TIMEOUT_MS = 60000;  // 1 minute
    private static final int HALF_OPEN_MAX_CALLS = 3;
    
    public CircuitBreaker getOrCreate(String domain) {
        return breakers.computeIfAbsent(domain, k -> new CircuitBreaker(domain));
    }
    
    public boolean isAllowed(String domain) {
        CircuitBreaker breaker = getOrCreate(domain);
        return breaker.allowRequest();
    }
    
    public void recordSuccess(String domain) {
        CircuitBreaker breaker = get(domain);
        if (breaker != null) {
            breaker.onSuccess();
        }
    }
    
    public void recordFailure(String domain) {
        CircuitBreaker breaker = getOrCreate(domain);
        breaker.onFailure();
    }
}
```

### Circuit Breaker Class

```java
public class CircuitBreaker {
    
    private final String domain;
    private CircuitBreakerState state;
    private int failureCount;
    private long openedAt;
    private int halfOpenSuccesses;
    
    public CircuitBreaker(String domain) {
        this.domain = domain;
        this.state = CircuitBreakerState.CLOSED;
        this.failureCount = 0;
    }
    
    public synchronized boolean allowRequest() {
        switch (state) {
            case CLOSED:
                return true;
                
            case OPEN:
                // Check if timeout elapsed
                if (System.currentTimeMillis() - openedAt > TIMEOUT_MS) {
                    log.info("Circuit breaker {} transitioning to HALF_OPEN", domain);
                    state = CircuitBreakerState.HALF_OPEN;
                    halfOpenSuccesses = 0;
                    return true;
                }
                return false;  // Still open, reject
                
            case HALF_OPEN:
                // Allow limited requests
                return halfOpenSuccesses < HALF_OPEN_MAX_CALLS;
                
            default:
                return false;
        }
    }
    
    public synchronized void onSuccess() {
        switch (state) {
            case CLOSED:
                failureCount = 0;
                break;
                
            case HALF_OPEN:
                halfOpenSuccesses++;
                if (halfOpenSuccesses >= HALF_OPEN_MAX_CALLS) {
                    log.info("Circuit breaker {} recovered, transitioning to CLOSED", domain);
                    state = CircuitBreakerState.CLOSED;
                    failureCount = 0;
                }
                break;
                
            case OPEN:
                // Shouldn't happen, but reset if it does
                log.warn("Success on OPEN circuit breaker {}", domain);
                state = CircuitBreakerState.CLOSED;
                failureCount = 0;
                break;
        }
    }
    
    public synchronized void onFailure() {
        switch (state) {
            case CLOSED:
                failureCount++;
                if (failureCount >= FAILURE_THRESHOLD) {
                    log.warn("Circuit breaker {} OPEN after {} failures", domain, failureCount);
                    state = CircuitBreakerState.OPEN;
                    openedAt = System.currentTimeMillis();
                }
                break;
                
            case HALF_OPEN:
                log.warn("Circuit breaker {} failed during HALF_OPEN, returning to OPEN", domain);
                state = CircuitBreakerState.OPEN;
                openedAt = System.currentTimeMillis();
                break;
                
            case OPEN:
                // Already open, nothing to do
                break;
        }
    }
    
    public CircuitBreakerStats getStats() {
        return CircuitBreakerStats.builder()
            .domain(domain)
            .state(state)
            .failureCount(failureCount)
            .openedAt(openedAt)
            .build();
    }
}
```

---

## Usage in Crawler

```java
@Service
public class CrawlerWorkerService {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakers;
    
    public void crawl(CrawlRequest request) {
        // Check circuit breaker
        if (!circuitBreakers.isAllowed(request.getDomain())) {
            log.info("Circuit breaker OPEN for {}, skipping", request.getDomain());
            return;
        }
        
        try {
            // Attempt crawl
            HttpResponse<String> response = fetchPage(request.getUrl());
            
            // Success
            circuitBreakers.recordSuccess(request.getDomain());
            processPage(response);
            
        } catch (IOException e) {
            // Failure
            log.error("Failed to crawl {}", request.getUrl(), e);
            circuitBreakers.recordFailure(request.getDomain());
        }
    }
}
```

---

## Metrics Integration

```java
@Service
public class MonitoringService {
    
    @Scheduled(fixedDelay = 30000)
    public void recordCircuitBreakerMetrics() {
        circuitBreakerRegistry.getAll().forEach(breaker -> {
            CircuitBreakerStats stats = breaker.getStats();
            
            // Prometheus metrics
            Gauge.builder("circuit_breaker_state", stats, s -> s.getState().ordinal())
                .tag("domain", stats.getDomain())
                .register(meterRegistry);
            
            Counter.builder("circuit_breaker_failures")
                .tag("domain", stats.getDomain())
                .register(meterRegistry)
                .increment(stats.getFailureCount());
        });
    }
}
```

---

## Admin API

```java
@GetMapping("/circuit-breakers")
public List<CircuitBreakerStats> getAllCircuitBreakers() {
    return circuitBreakerRegistry.getAll().stream()
        .map(CircuitBreaker::getStats)
        .collect(Collectors.toList());
}

@PostMapping("/circuit-breakers/{domain}/reset")
public void resetCircuitBreaker(@PathVariable String domain) {
    circuitBreakerRegistry.getOrCreate(domain).reset();
}
```

---

*Circuit breakers prevent cascading failures and enable automatic recovery!*
