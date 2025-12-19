# Distributed Locking with Redlock

> **File:** [DistributedLockService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/DistributedLockService.java)  
> **Algorithm:** Redlock for distributed coordination

---

## Problem

**Scenario:** Multiple crawler instances shouldn't crawl same domain simultaneously

**Race Condition (WRONG):**
```java
if (!redis.exists("lock:example.com")) {
    redis.set("lock:example.com", "worker-1");
    crawl();  // ← TWO WORKERS MAY REACH HERE!
}
```

---

## Redlock Algorithm

### Atomic Lock Acquisition

```java
@Service
public class DistributedLockService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean acquireLock(String resource, String owner, long ttlMs) {
        String lockKey = "lock:" + resource;
        
        // Lua script ensures atomicity
        String luaScript =
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
            "  redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(lockKey),
            owner,  // Unique ID per worker
            ttlMs   // Auto-expire (safety)
        );
        
        return result != null && result == 1;
    }
    
    public boolean releaseLock(String resource, String owner) {
        String lockKey = "lock:" + resource;
        
        // Only owner can release (prevent accidental unlock)
        String luaScript =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(lockKey),
            owner
        );
        
        return result != null && result == 1;
    }
}
```

---

## Usage Pattern

```java
@Service
public class CrawlSchedulerService {
    
    public void crawlDomain(String domain) {
        String lockId = UUID.randomUUID().toString();
        
        boolean acquired = lockService.acquireLock(
            "domain:" + domain,
            lockId,
            30000  // 30 seconds
        );
        
        if (acquired) {
            try {
                // Only this worker can crawl this domain
                List<CrawlUrl> urls = getUrlsForDomain(domain);
                for (CrawlUrl url : urls) {
                    crawl(url);
                }
            } finally {
                // Always release
                lockService.releaseLock("domain:" + domain, lockId);
            }
        } else {
            log.info("Domain {} locked by another worker", domain);
        }
    }
}
```

---

## Lock with Retry

```java
public boolean acquireLockWithRetry(String resource, String owner,
                                   long ttlMs, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        if (acquireLock(resource, owner, ttlMs)) {
            return true;
        }
        
        // Exponential backoff
        try {
            Thread.sleep((long) (Math.pow(2, i) * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    return false;
}
```

---

## Edge Cases

### 1. Worker Crashes While Holding Lock

**Problem:** Lock never released → deadlock

**Solution:** TTL auto-expires lock
```java
// Lock expires after 30 seconds even if worker crashes
acquireLock("resource", "worker-1", 30000);
```

### 2. Network Partition

**Problem:** Worker A thinks it has lock, Worker B also acquires

**Solution:** Acceptable for crawling (at-least-once is OK)

For critical operations: Use consensus (Raft/Paxos)

### 3. Clock Skew

**Problem:** TTL inconsistent across servers

**Solution:** Use Redis server time (PEXPIRE is server-side)

---

## Multi-Redis Redlock (Production)

```java
public class MultiRedisLockService {
    
    private final List<RedisTemplate> redisInstances;
    
    public boolean acquireLock(String resource, String owner, long ttlMs) {
        int quorum = redisInstances.size() / 2 + 1;
        int acquired = 0;
        
        // Try to acquire lock on all instances
        for (RedisTemplate redis : redisInstances) {
            if (tryAcquire(redis, resource, owner, ttlMs)) {
                acquired++;
            }
        }
        
        // Success if majority acquired
        if (acquired >= quorum) {
            return true;
        } else {
            // Rollback: release all
            for (RedisTemplate redis : redisInstances) {
                tryRelease(redis, resource, owner);
            }
            return false;
        }
    }
}
```

---

*Distributed locks enable safe coordination across crawler instances!*
