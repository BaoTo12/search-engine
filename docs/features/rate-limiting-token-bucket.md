# Token Bucket Rate Limiter

> **File:** [TokenBucketRateLimiter.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/TokenBucketRateLimiter.java)  
> **Algorithm:** Distributed rate limiting with Redis

---

## Algorithm

### Token Bucket Model

```
Bucket:
├─ Capacity: 10 tokens (max burst)
├─ Refill rate: 1 token/second
└─ Current tokens: X

Request arrives:
  if tokens > 0:
    tokens -= 1
    allow request
  else:
    reject (rate limited)

Background: tokens refilled at constant rate
```

### Why Token Bucket?

**vs Fixed Window:**
- Allows burst traffic
- Smooth rate limiting
- No boundary issues

**vs Sliding Window:**
- Simpler implementation
- Lower memory usage
- Predictable burst capacity

---

## Redis-Based Implementation

### Lua Script (Atomic)

```lua
-- token_bucket.lua
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Get current state
local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1]) or capacity
local last_refill = tonumber(bucket[2]) or now

-- Calculate refill
local elapsed = now - last_refill
local refill_amount = elapsed * rate
tokens = math.min(capacity, tokens + refill_amount)

-- Try to consume token
if tokens >= 1 then
    tokens = tokens - 1
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('EXPIRE', key, 3600)  -- 1 hour TTL
    return 1  -- Allowed
else
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
    return 0  -- Rejected
end
```

### Java Implementation

```java
@Service
public class TokenBucketRateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private final DefaultRedisScript<Long> luaScript;
    
    // Per-domain configuration
    private static final int CAPACITY = 10;       // Max burst
    private static final double RATE = 1.0;      // 1 token/second
    
    @PostConstruct
    public void init() {
        // Load Lua script
        luaScript = new DefaultRedisScript<>();
        luaScript.setScriptText(loadScript("token_bucket.lua"));
        luaScript.setResultType(Long.class);
    }
    
    public boolean isAllowed(String domain) {
        String key = "rate_limit:token_bucket:" + domain;
        long now = System.currentTimeMillis() / 1000;  // Seconds
        
        Long result = redisTemplate.execute(
            luaScript,
            Collections.singletonList(key),
            String.valueOf(CAPACITY),
            String.valueOf(RATE),
            String.valueOf(now)
        );
        
        return result != null && result == 1;
    }
    
    public void waitIfNeeded(String domain) throws InterruptedException {
        while (!isAllowed(domain)) {
            // Wait 1 second for token refill
            Thread.sleep(1000);
        }
    }
    
    public TokenBucketStats getStats(String domain) {
        String key = "rate_limit:token_bucket:" + domain;
        Map<Object, Object> bucket = redisTemplate.opsForHash().entries(key);
        
        return TokenBucketStats.builder()
            .domain(domain)
            .tokens(Double.parseDouble((String) bucket.getOrDefault("tokens", "0")))
            .capacity(CAPACITY)
            .rate(RATE)
            .lastRefill(Long.parseLong((String) bucket.getOrDefault("last_refill", "0")))
            .build();
    }
}
```

---

## Adaptive Rate Limiting

### Dynamic Rate Adjustment

```java
@Service
public class AdaptiveRateLimiter {
    
    public RateLimitConfig getRateConfig(String domain) {
        DomainMetadata metadata = domainRepository.findByDomain(domain);
        
        if (metadata == null) {
            return RateLimitConfig.DEFAULT;
        }
        
        // Adjust based on domain behavior
        double successRate = metadata.getSuccessRate();
        int errorCount = metadata.getRecentErrorCount();
        
        if (errorCount > 10 || successRate < 0.5) {
            // Aggressive throttling
            return RateLimitConfig.builder()
                .capacity(5)
                .rate(0.5)  // 1 request per 2 seconds
                .build();
                
        } else if (successRate > 0.95) {
            // Relaxed limits
            return RateLimitConfig.builder()
                .capacity(20)
                .rate(2.0)  // 2 requests/second
                .build();
        }
        
        return RateLimitConfig.DEFAULT;
    }
}
```

---

## Robots.txt Integration

```java
public void respectCrawlDelay(String domain, String robotsTxt) {
    // Parse crawl-delay directive
    Pattern pattern = Pattern.compile("Crawl-delay:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(robotsTxt);
    
    if (matcher.find()) {
        int crawlDelay = Integer.parseInt(matcher.group(1));
        
        // Update rate limit config
        RateLimitConfig config = RateLimitConfig.builder()
            .capacity(1)
            .rate(1.0 / crawlDelay)  // 1 request per crawl-delay seconds
            .build();
        
        saveRateLimitConfig(domain, config);
    }
}
```

---

## Monitoring

```java
@RestController
@RequestMapping("/api/v1/admin/rate-limit")
public class RateLimitController {
    
    @GetMapping("/stats/{domain}")
    public TokenBucketStats getStats(@PathVariable String domain) {
        return rateLimiter.getStats(domain);
    }
    
    @GetMapping("/blocked")
    public List<String> getBlockedDomains() {
        // Domains with 0 tokens
        return domainRepository.findAll().stream()
            .filter(d -> rateLimiter.getStats(d.getDomain()).getTokens() == 0)
            .map(DomainMetadata::getDomain)
            .collect(Collectors.toList());
    }
}
```

---

*Token bucket rate limiting ensures polite, distributed crawling!*
