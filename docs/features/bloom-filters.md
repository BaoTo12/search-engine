# Bloom Filters - Probabilistic URL Deduplication

> **Difficulty:** Intermediate  
> **File:** [BloomFilterService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/BloomFilterService.java)  
> **Data Structure:** Probabilistic set membership

---

## 🎯 Problem & Solution

**Challenge:** Track which URLs have been crawled (10M+ URLs)

**Naive Approach (Hash Set):**
```java
Set<String> visitedUrls = new HashSet<>();  // O(1) lookup
// Memory: 10M URLs × 64 bytes/URL ≈ 640 MB
```

**Bloom Filter:**
```java
BloomFilter<String> visited = BloomFilter.create(...);
// Memory: 10M URLs × 1.2 bytes/URL ≈ 12 MB
// Speedup: 53x less memory!
```

---

## 📐 Mathematical Foundation

### False Positive Rate Formula

```
FPR = (1 - e^(-kn/m))^k

Where:
- k = number of hash functions
- n = number of elements inserted
- m = size of bit array
- e ≈ 2.71828 (Euler's number)
```

### Derivation

**Step 1:** Probability single bit is NOT set by one hash function
```
P(bit not set) = 1 - 1/m
```

**Step 2:** After k hash functions on one element
```
P(bit not set) = (1 - 1/m)^k
```

**Step 3:** After n elements inserted
```
P(bit not set) = (1 - 1/m)^(kn)
```

**Step 4:** Approximate for large m
```
P(bit not set) ≈ e^(-kn/m)
```

**Step 5:** False positive (all k bits set by chance)
```
FPR = (1 - e^(-kn/m))^k
```

### Optimal Parameters

**Optimal number of hash functions:**
```
k_optimal = (m/n) × ln(2) ≈ 0.693 × (m/n)
```

**Bit array size for target FPR:**
```
m = -n × ln(FPR) / (ln(2))²

Example: 10M URLs, 1% FPR
m = -10,000,000 × ln(0.01) / (ln(2))²
  ≈ 95,850,584 bits
  ≈ 11.5 MB

k = 0.693 × (95,850,584 / 10,000,000)
  ≈ 7 hash functions
```

---

## 💻 Implementation

### Using Google Guava

```java
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

@Service
public class BloomFilterService {
    
    private BloomFilter<String> urlFilter;
    private final RedisTemplate<String, String> redisTemplate;
    
    // Configuration
    private static final int EXPECTED_INSERTIONS = 10_000_000;
    private static final double FALSE_POSITIVE_RATE = 0.01;  // 1%
    
    @PostConstruct
    public void init() {
        urlFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            EXPECTED_INSERTIONS,
            FALSE_POSITIVE_RATE
        );
        
        log.info("Bloom filter initialized: {} expected URLs, {}% FPR",
                EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE * 100);
    }
    
    /**
     * Check if URL might have been visited
     * Returns:
     * - false: DEFINITELY not visited
     * - true: PROBABLY visited (may be false positive)
     */
    public boolean mightContain(String url) {
        return urlFilter.mightContain(url);
    }
    
    /**
     * Mark URL as visited
     */
    public void markVisited(String url) {
        urlFilter.put(url);
    }
    
    /**
     * Get current size and FPR
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Approximate number of insertions
        long approximateElementCount = urlFilter.approximateElementCount();
        
        // Current FPR (increases as more elements added)
        double currentFPR = urlFilter.expectedFpp();
        
        stats.put("approximate_count", approximateElementCount);
        stats.put("expected_fpr", FALSE_POSITIVE_RATE);
        stats.put("current_fpr", currentFPR);
        stats.put("capacity", EXPECTED_INSERTIONS);
        
        return stats;
    }
}
```

### Two-Layer Verification Strategy

**Problem:** 1% false positive rate means 100,000 errors for 10M URLs

**Solution:** Bloom Filter + Redis fallback

```java
public boolean hasBeenCrawled(String url) {
    // Layer 1: Bloom filter (fast, in-memory)
    if (!urlFilter.mightContain(url)) {
        return false;  // DEFINITELY not crawled (0% error)
    }
    
    // Layer 2: Redis verification (eliminate false positives)
    String urlHash = hashUrl(url);
    Boolean exists = redisTemplate.hasKey("visited:" + urlHash);
    
    return Boolean.TRUE.equals(exists);
}

public void markAsCrawled(String url) {
    String urlHash = hashUrl(url);
    
    // Add to both layers
    urlFilter.put(url);                    // Probabilistic
    redisTemplate.opsForValue().set(
        "visited:" + urlHash,
        "1",
        30,
        TimeUnit.DAYS
    );  // Ground truth with TTL
}
```

**Performance:**
```
99% of lookups: Bloom filter only (no network call)
1% of lookups: Bloom filter + Redis (one network call)

Average latency:
- Bloom filter: 100ns
- Redis: 1ms
- Weighted average: 0.99 × 100ns + 0.01 × 1ms ≈ 11µs
```

---

## 📊 Performance Analysis

### Memory Comparison

| Method | 10M URLs | Memory | Per URL |
|--------|----------|---------|---------|
| **HashSet<String>** | 10M | ~640 MB | 64 bytes |
| **HashSet<SHA256>** | 10M | ~320 MB | 32 bytes |
| **Bloom Filter (1% FPR)** | 10M | ~12 MB | 1.2 bytes |
| **Bloom Filter (0.1% FPR)** | 10M | ~18 MB | 1.8 bytes |

**Savings:** 53x less memory vs HashSet!

### Time Complexity

| Operation | Hash Set | Bloom Filter |
|-----------|----------|--------------|
| **Insert** | O(1) | O(k) ≈ O(1) |
| **Lookup** | O(1) | O(k) ≈ O(1) |
| **Delete** | O(1) | **Not supported** |

**Note:** Bloom filters don't support deletion (counting Bloom filters do, but larger)

---

## 🔍 Hash Functions

### MurmurHash3 (Used by Guava)

```java
// Guava uses MurmurHash3 internally
// Fast, non-cryptographic hash with good distribution

public class CustomBloomFilter {
    private BitSet bitArray;
    private int k;  // Number of hash functions
    private int m;  // Bit array size
    
    private int[] getHashes(String element) {
        int[] hashes = new int[k];
        
        // Generate k hashes using double hashing
        int hash1 = MurmurHash3.hash32(element.getBytes(), 0);
        int hash2 = MurmurHash3.hash32(element.getBytes(), hash1);
        
        for (int i = 0; i < k; i++) {
            // h_i = (hash1 + i × hash2) mod m
            hashes[i] = Math.abs((hash1 + i * hash2) % m);
        }
        
        return hashes;
    }
    
    public void add(String element) {
        for (int hash : getHashes(element)) {
            bitArray.set(hash);
        }
    }
    
    public boolean mightContain(String element) {
        for (int hash : getHashes(element)) {
            if (!bitArray.get(hash)) {
                return false;  // Definitely not in set
            }
        }
        return true;  // Probably in set
    }
}
```

---

## 🎯 Use Cases

### 1. URL Deduplication in Crawler

```java
@Service
public class CrawlSchedulerService {
    
    @Autowired
    private BloomFilterService bloomFilter;
    
    public void addSeedUrls(List<String> urls) {
        for (String url : urls) {
            String normalized = normalizeUrl(url);
            
            // Quick check with Bloom filter
            if (bloomFilter.hasBeenCrawled(normalized)) {
                log.debug("URL already crawled (Bloom filter): {}", url);
                continue;  // Skip 99% of duplicates instantly
            }
            
            // Add to crawl queue
            crawlUrlRepository.save(new CrawlUrl(normalized));
            bloomFilter.markAsCrawled(normalized);
        }
    }
}
```

### 2. Email Spam Detection

```java
// Track known spam emails
BloomFilter<String> spamFilter = BloomFilter.create(...);

public boolean isSpam(String emailHash) {
    if (spamFilter.mightContain(emailHash)) {
        // Probably spam - verify with database
        return spamRepository.exists(emailHash);
    }
    return false;  // Definitely not spam
}
```

### 3. Cache Lookup Optimization

```java
// Avoid cache misses for keys that don't exist
BloomFilter<String> cacheKeys = BloomFilter.create(...);

public Optional<Value> get(String key) {
    if (!cacheKeys.mightContain(key)) {
        return Optional.empty();  // Skip expensive cache lookup
    }
    return cache.get(key);
}

public void put(String key, Value value) {
    cache.put(key, value);
    cacheKeys.put(key);
}
```

---

## 🚨 Limitations & Mitigation

### Limitation 1: No Deletion

**Problem:** Can't remove elements once added

**Workarounds:**
1. **Counting Bloom Filter:** Track count per bit (4-8x more memory)
2. **Rebuild periodically:** Create new filter, discard old
3. **TTL strategy:** Assume elements expire after time

### Limitation 2: False Positives

**Problem:** 1% FPR = 100K errors for 10M elements

**Mitigation:** Two-layer verification (Bloom + Redis)

### Limitation 3: Fixed Capacity

**Problem:** FPR increases if more elements added than expected

**Detection:**
```java
if (bloomFilter.approximateElementCount() > EXPECTED_INSERTIONS * 0.9) {
    log.warn("Bloom filter nearing capacity, FPR increasing");
    // Consider rebuilding with larger capacity
}
```

---

## 📈 Production Tuning

### Choosing FPR

| FPR | Use Case | Memory per URL |
|-----|----------|----------------|
| 10% | Caching hints | 0.6 bytes |
| 1% | **Crawler (default)** | 1.2 bytes |
| 0.1% | Critical deduplication | 1.8 bytes |
| 0.01% | Financial transactions | 2.4 bytes |

### Capacity Planning

```java
// Calculate required memory
public static long calculateMemoryBytes(int expectedElements, double fpr) {
    double bitsPerElement = -Math.log(fpr) / (Math.log(2) * Math.log(2));
    long totalBits = (long)(expectedElements * bitsPerElement);
    return totalBits / 8;  // Convert to bytes
}

// Example
long memory = calculateMemoryBytes(10_000_000, 0.01);
// Result: ~11.98 MB
```

### Serialization for Persistence

```java
// Save Bloom filter to disk for crash recovery
public void saveToFile(String filepath) throws IOException {
    try ( OutputStream out = new FileOutputStream(filepath)) {
        urlFilter.writeTo(out);
    }
}

// Load from disk
public void loadFromFile(String filepath) throws IOException {
    try (InputStream in = new FileInputStream(filepath)) {
        urlFilter = BloomFilter.readFrom(in, Funnels.stringFunnel(Charset.defaultCharset()));
    }
}
```

---

*Bloom filters provide memory-efficient set membership testing - essential for large-scale systems!*
