# 🎓 Enterprise Search Engine - Advanced Deep Dive

> **For:** Experienced developers who know Java/Spring Boot
> 
> **Focus:** Algorithms, distributed systems, search engine internals, production optimizations

---

## Table of Contents

1. [PageRank Algorithm - Deep Implementation Analysis](#1-pagerank-deep-dive)
2. [URL Prioritization Strategies - Mathematical Models](#2-url-prioritization-strategies)
3. [Content Deduplication - SimHash & LSH](#3-content-deduplication)
4. [Distributed Crawling - Kafka Partitioning & Consistency](#4-distributed-crawling)
5. [Query Processing Pipeline - From Input to Results](#5-query-processing-pipeline)
6. [Bloom Filters - Probabilistic Data Structures](#6-bloom-filters)
7. [Distributed Locking - Redlock Implementation](#7-distributed-locking)
8. [Circuit Breakers - Resilience Patterns](#8-circuit-breakers)
9. [Rate Limiting - Token Bucket with Redis](#9-rate-limiting)
10. [Robots.txt Parsing - RFC 9309 Compliance](#10-robotstxt-parsing)
11. [URL Normalization - Canonical Forms](#11-url-normalization)
12. [Production Optimizations & Tuning](#12-production-optimizations)

---

## 1. PageRank Deep Dive

### Mathematical Foundation

**Core Formula:**
```
PR(A) = (1-d) + d × Σ(PR(Ti) / C(Ti))

Where:
- PR(A) = PageRank of page A
- d = damping factor (0.85) - probability user clicks a link vs random jump
- Ti = pages linking to A
- C(Ti) = total outbound links from Ti
- Σ = sum over all incoming links
```

**Matrix Formulation:**
```
PR = (1-d) × E + d × M × PR

Where:
- E = [1/N, 1/N, ..., 1/N]ᵀ (random jump vector)
- M = transition matrix (M[i][j] = 1/outbound_count[i] if i→j link exists)
- PR = PageRank vector
```

### Implementation Analysis

[PageRankService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/PageRankService.java)

**1. Link Graph Construction:**
```java
private Map<String, Set<String>> buildLinkGraph() {
    // Query ALL page_links in single query (avoid N+1)
    List<PageLink> allLinks = pageLinkRepository.findAll();
    
    // Build adjacency list: target → Set<sources>
    Map<String, Set<String>> incomingLinks = new HashMap<>();
    
    for (PageLink link : allLinks) {
        incomingLinks
            .computeIfAbsent(link.getTargetUrl(), k -> new HashSet<>())
            .add(link.getSourceUrl());
    }
    
    return incomingLinks;  // O(E) where E = number of edges
}
```

**Performance Consideration:**
- Single database query loads entire graph into memory
- Trade-off: Memory vs. repeated DB queries
- For 1M pages × 10 links/page = 10M edges ≈ 800MB RAM
- Alternative for massive scale: Graph databases (Neo4j) or distributed PageRank (Pregel/GraphX)

**2. Iterative Calculation (Power Iteration):**
```java
public void calculatePageRank() {
    Map<String, Set<String>> incomingLinks = buildLinkGraph();
    Map<String, Integer> outboundCounts = countOutboundLinks();
    
    // Initialize: uniform distribution
    Map<String, Double> ranks = new HashMap<>();
    int N = incomingLinks.keySet().size();
    double initialRank = 1.0 / N;
    for (String url : incomingLinks.keySet()) {
        ranks.put(url, initialRank);
    }
    
    final double d = 0.85;
    final double oneMinusD = 0.15;
    final int maxIterations = 100;
    final double epsilon = 0.0001;  // Convergence threshold
    
    for (int iter = 0; iter < maxIterations; iter++) {
        Map<String, Double> newRanks = new HashMap<>();
        double totalChange = 0.0;
        
        for (String page : incomingLinks.keySet()) {
            // Random jump component
            double rank = oneMinusD / N;
            
            // Link flow component
            Set<String> incoming = incomingLinks.get(page);
            for (String incomingPage : incoming) {
                int outbound = outboundCounts.getOrDefault(incoming Page, 1);
                rank += d * (ranks.get(incomingPage) / outbound);
            }
            
            newRanks.put(page, rank);
            totalChange += Math.abs(rank - ranks.get(page));
        }
        
        ranks = newRanks;
        
        // Convergence check: L1 norm < epsilon
        if (totalChange < epsilon) {
            log.info("Converged in {} iterations", iter + 1);
            break;
        }
    }
    
    // Normalize to [0, 1]
    double maxRank = Collections.max(ranks.values());
    ranks.replaceAll((k, v) -> v / maxRank);
    
    // Batch save
    List<PageRankEntity> entities = ranks.entrySet().stream()
        .map(e -> new PageRankEntity(e.getKey(), e.getValue(), LocalDateTime.now()))
        .collect(Collectors.toList());
    
    pageRankRepository.saveAll(entities);  // Single transaction
}
```

**Convergence Analysis:**
- **Why it converges:** Markov chain is irreducible + aperiodic (due to damping)
- **Convergence rate:** O(log(1/ε)) iterations where ε = error tolerance
- **Typical iterations:** 50-100 for web-scale graphs
- **Optimization:** Can use previous iteration's results as starting point for incremental updates

**Edge Cases Handled:**
1. **Dangling nodes** (no outbound links): Distribute their rank evenly
2. **Disconnected components:** Random jump ensures reachability
3. **Rank sinks** (only incoming links): Damping factor prevents accumulation

**Production Optimizations:**
```java
// 1. Sparse matrix representation (most pages have < 100 links)
Map<String, List<WeightedEdge>> sparseGraph = ...;

// 2. Block-based iteration (for graphs > RAM)
processBlocks(int blockSize = 100000) {
    for (int i = 0; i < totalPages; i += blockSize) {
        // Process pages [i, i+blockSize)
    }
}

// 3. Incremental PageRank for dynamic graphs
// Only recalculate affected subgraph when links change
```

---

## 2. URL Prioritization Strategies

### OPIC (Online Page Importance Computation)

**Algorithm Philosophy:**
- **Problem:** Full PageRank requires entire link graph (expensive)
- **Solution:** Local "cash flow" model - no global computation needed

**Implementation:** [OPICStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/OPICStrategy.java)

```java
public class OPICStrategy implements URLPrioritizationStrategy {
    
    // Redis stores: opic:cash:<url> → current cash value
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final double INITIAL_CASH = 1.0;
    private static final String CASH_PREFIX = "opic:cash:";
    
    @Override
    public double calculatePriority(String url, int depth, 
                                    Double pageRank, String domain) {
        // Priority = accumulated cash
        String cashKey = CASH_PREFIX + hashUrl(url);
        String cashStr = redisTemplate.opsForValue().get(cashKey);
        
        return cashStr != null ? Double.parseDouble(cashStr) : INITIAL_CASH;
    }
    
    // Called after page is crawled
    public void distributeCash(String sourceUrl, List<String> outboundLinks) {
        if (outboundLinks.isEmpty()) return;
        
        // 1. Get source page's cash
        String sourceKey = CASH_PREFIX + hashUrl(sourceUrl);
        String cashStr = redisTemplate.opsForValue().get(sourceKey);
        double sourceCash = cashStr != null ? Double.parseDouble(cashStr) : INITIAL_CASH;
        
        // 2. Split evenly among outbound links
        double cashPerLink = sourceCash / outboundLinks.size();
        
        // 3. Atomic increment using Lua script
        String luaScript =
            "local current = redis.call('get', KEYS[1]) or '0' " +
            "local newVal = tonumber(current) + tonumber(ARGV[1]) " +
            "redis.call('set', KEYS[1], tostring(newVal)) " +
            "redis.call('expire', KEYS[1], 86400) " +  // 24h TTL
            "return newVal";
        
        for (String targetUrl : outboundLinks) {
            String targetKey = CASH_PREFIX + hashUrl(targetUrl);
            redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Double.class),
                Collections.singletonList(targetKey),
                String.valueOf(cashPerLink)
            );
        }
        
        // 4. Source cash is spent (reset to 0)
        redisTemplate.opsForValue().set(sourceKey, "0");
    }
}
```

**Mathematical Properties:**
```
Total cash in system = N × INITIAL_CASH (conserved)

After page i is crawled:
- Cash(i) → 0
- Cash(j) += Cash(i) / outbound_count(i) for each outbound link j

Steady state approximation of PageRank without global iteration!
```

**Advantages over PageRank:**
- **O(1) priority calculation** vs O(N × E) full PageRank
- **Real-time updates** - no batch recalculation
- **Distributed** - each page's cash independent

**Trade-off:**
- Less accurate than full PageRank
- Good enough for crawl prioritization

---

## 3. Content Deduplication

### SimHash Algorithm

**Problem:** Detect near-duplicate web pages (not just exact duplicates)

**Use Cases:**
- Mirror sites (example.com vs example.org with same content)
- Plagiarized content
- Templated pages (only header/footer different)

**Implementation:** [ContentDeduplicationService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/ContentDeduplicationService.java)

### SimHash Deep Dive

**Algorithm Steps:**

```java
public long calculateSimHash(String content) {
    // 1. Tokenize into features (shingles)
    List<String> features = extractFeatures(content);
    
    // 2. Initialize 64-bit vector
    int[] vector = new int[64];
    
    // 3. For each feature:
    for (String feature : features) {
        // Hash feature to 64-bit
        long hash = MurmurHash3.hash64(feature.getBytes());
        
        // Weight by TF-IDF (optional - use term frequency here)
        int weight = getTermFrequency(feature, content);
        
        // Update vector: +weight if bit=1, -weight if bit=0
        for (int i = 0; i < 64; i++) {
            if ((hash & (1L << i)) != 0) {
                vector[i] += weight;
            } else {
                vector[i] -= weight;
            }
        }
    }
    
    // 4. Generate final fingerprint
    long fingerprint = 0L;
    for (int i = 0; i < 64; i++) {
        if (vector[i] > 0) {
            fingerprint |= (1L << i);
        }
    }
    
    return fingerprint;
}

private List<String> extractFeatures(String content) {
    // N-grams (shingles) of size 5
    List<String> shingles = new ArrayList<>();
    String[] words = content.toLowerCase().split("\\s+");
    
    for (int i = 0; i <= words.length - 5; i++) {
        String shingle = String.join(" ", 
            Arrays.copyOfRange(words, i, i + 5));
        shingles.add(shingle);
    }
    
    return shingles;
}
```

**Similarity Detection:**
```java
public boolean isDuplicate(String content1, String content2) {
    long hash1 = calculateSimHash(content1);
    long hash2 = calculateSimHash(content2);
    
    // Hamming distance = number of differing bits
    int hammingDistance = Long.bitCount(hash1 ^ hash2);
    
    // Threshold: 3-bit difference = ~95% similar content
    return hammingDistance <= 3;
}
```

**Why SimHash Works:**
```
Property: Similar documents → similar fingerprints
- Documents with 90% overlap → Hamming distance ≤ 3
- Completely different → Hamming distance ≈ 32 (random)

XOR operation: finds bit differences in O(1)
Instead of comparing full text (O(N)), just compare 64-bit integers!
```

### Locality-Sensitive Hashing (LSH) for Scale

**Problem:** With 1B pages, can't compare every pair (1B² comparisons)

**Solution:** LSH bucketing

```java
public class LSHIndex {
    // Split 64-bit fingerprint into 4 × 16-bit chunks
    // Pages in same bucket for ANY chunk are candidates
    
    private Map<Integer, Set<String>> buckets = new HashMap<>();
    
    public void index(String url, long simhash) {
        // Extract 4 × 16-bit segments
        for (int i = 0; i < 4; i++) {
            int segment = (int)((simhash >> (i * 16)) & 0xFFFF);
            buckets.computeIfAbsent(segment, k -> new HashSet<>())
                   .add(url);
        }
    }
    
    public Set<String> findCandidates(long querySimhash) {
        Set<String> candidates = new HashSet<>();
        
        for (int i = 0; i < 4; i++) {
            int segment = (int)((querySimhash >> (i * 16)) & 0xFFFF);
            if (buckets.containsKey(segment)) {
                candidates.addAll(buckets.get(segment));
            }
        }
        
        return candidates;  // Then verify with exact Hamming distance
    }
}
```

**Performance:**
- Index insertion: O(1)
- Candidate retrieval: O(k) where k = average bucket size
- Reduces comparisons from O(N) to O(k) where k << N

---

## 4. Distributed Crawling

### Kafka Partitioning Strategy

**Goal:** Ensure same domain always goes to same consumer (for rate limiting)

**Implementation:**

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, CrawlRequest> crawlRequestProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, 
                  DomainPartitioner.class.getName());
        
        return new DefaultKafkaProducerFactory<>(config);
    }
}

public class DomainPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        // key = domain name
        String domain = (String) key;
        
        // Consistent hashing: hash(domain) % num_partitions
        int hashCode = domain.hashCode();
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        return Math.abs(hashCode % numPartitions);
    }
}
```

**Why This Works:**
```
domain "example.com" → hash → partition 3 → consumer C3
domain "example.com" → hash → partition 3 → consumer C3 (same!)

Benefits:
1. Single consumer per domain = natural rate limiting
2. No distributed coordination needed
3. Automatic load balancing (Kafka rebalancing)
```

**Consumer Configuration:**
```java
@KafkaListener(
    topics = "crawl-requests",
    groupId = "crawler-workers",
    concurrency = "10"  // 10 parallel consumers
)
public void processCrawlRequest(
    @Payload CrawlRequest request,
    @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition
) {
    // This consumer handles specific partitions
    // Same domain always arrives here
    
    // Safe to maintain per-domain state in memory
    rateLimiter.waitIfNeeded(request.getDomain());
    crawl(request);
}
```

### Handling Consumer Failures

**Scenario:** Consumer crashes mid-crawl

**Solution:** Kafka offset management + idempotency

```java
@KafkaListener(
    topics = "crawl-requests",
    containerFactory = "manualAckContainerFactory"
)
public void processCrawlRequest(
    CrawlRequest request,
    Acknowledgment ack
) {
    try {
        // 1. Check if already processed (idempotency)
        if (bloomFilter.mightContain(request.getUrl())) {
            String urlHash = hashUrl(request.getUrl());
            if (crawlUrlRepository.existsByUrlHash(urlHash)) {
                ack.acknowledge();  // Skip, already done
                return;
            }
        }
        
        // 2. Process
        crawlPage(request);
        
        // 3. Mark complete in DB
        updateCrawlStatus(request.getUrl(), CrawlStatus.COMPLETED);
        
        // 4. Commit Kafka offset (manual ack)
        ack.acknowledge();
        
    } catch (Exception e) {
        // DON'T acknowledge - message will be redelivered
        log.error("Crawl failed, will retry", e);
        
        // Optionally: Send to dead-letter queue after N retries
        if (getRetryCount(request) > MAX_RETRIES) {
            sendToDeadLetterQueue(request);
            ack.acknowledge();
        }
    }
}
```

**Exactly-Once Semantics:**
```
1. Deduplication check (Bloom filter + DB)
2. Process message
3. Update DB within transaction
4. Commit Kafka offset

If crash occurs before step 4:
- Message redelivered
- Step 1 detects duplicate
- Skip processing
```

---

## 5. Query Processing Pipeline

### End-to-End Flow

```
User Query: "java concurency tutoral" (typos!)
    ↓
Query Expansion Service
    ↓
{
  original: "java concurency tutoral",
  corrected: "java concurrency tutorial",
  synonyms: ["multithreading", "parallel processing"],
  entities: ["Java"],
  intent: "TUTORIAL"
}
    ↓
Elasticsearch Query Builder
    ↓
{
  bool: {
    should: [
      { match: { title: { query: "java concurrency tutorial", boost: 3 }}},
      { match: { content: "java concurrency tutorial" }},
      { match: { content: "java multithreading" }},
      { match: { content: "java parallel processing" }}
    ],
    filter: [
      { term: { detected_language: "en" }}
    ]
  },
  function_score: {
    functions: [
      { field_value_factor: { field: "pageRank", modifier: "log1p" }}
    ],
    boost_mode: "multiply"
  }
}
    ↓
Elasticsearch Execution
    ↓
Raw Results (sorted by relevance × PageRank)
    ↓
Post-Processing
    ↓
{
  results: [...],
  corrected_query: "java concurrency tutorial",
  did_you_mean: "Did you mean: java concurrency tutorial?",
  related_searches: ["java threads", "java executor service"]
}
```

### Spell Correction Implementation

```java
public class QueryExpansionService {
    
    // Levenshtein distance with dynamic programming
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i-1][j], dp[i][j-1]),
                        dp[i-1][j-1]
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    public String correctSpelling(String word) {
        // Check against dictionary of common terms
        // (built from indexed document vocabulary)
        
        if (dictionary.contains(word)) {
            return word;  // Correct already
        }
        
        // Find closest match with edit distance ≤ 2
        String bestMatch = word;
        int minDistance = Integer.MAX_VALUE;
        
        for (String dictWord : dictionary) {
            // Optimization: skip if length difference > 2
            if (Math.abs(word.length() - dictWord.length()) > 2) {
                continue;
            }
            
            int distance = levenshteinDistance(word, dictWord);
            
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                bestMatch = dictWord;
            }
        }
        
        return bestMatch;
    }
}
```

**Optimization:** Use BK-Tree for O(log n) dictionary lookups instead of O(n)

---

## 6. Bloom Filters

### Mathematical Foundation

**False Positive Rate:**
```
P(false_positive) = (1 - e^(-kn/m))^k

Where:
- k = number of hash functions
- n = number of inserted elements
- m = bit array size

Optimal k = (m/n) × ln(2)  ≈  0.693 × (m/n)
```

**Example:** For 10M URLs with 1% FPR:
```
m = -n × ln(p) / (ln(2))²
  = -10,000,000 × ln(0.01) / (ln(2))²
  ≈ 95,850,584 bits
  ≈ 11.5 MB

k = (m/n) × ln(2)
  = (95,850,584 / 10,000,000) × 0.693
  ≈ 7 hash functions
```

### Implementation with Guava

```java
@Service
public class BloomFilterService {
    
    private BloomFilter<String> urlFilter;
    private final RedisTemplate<String, String> redisTemplate;
    
    @PostConstruct
    public void init() {
        // 10M expected insertions, 1% FPR
        urlFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            10_000_000,
            0.01
        );
    }
    
    public boolean mightContain(String url) {
        // 1. Check Bloom filter (fast, in-memory)
        if (!urlFilter.mightContain(url)) {
            return false;  // Definitely not seen
        }
        
        // 2. Possible false positive - verify in Redis
        String urlHash = hashUrl(url);
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("visited:" + urlHash)
        );
    }
    
    public void markVisited(String url) {
        String urlHash = hashUrl(url);
        
        // Add to both
        urlFilter.put(url);  // Probabilistic
        redisTemplate.opsForValue().set(
            "visited:" + urlHash, 
            "1", 
            30, 
            TimeUnit.DAYS
        );  // Ground truth with TTL
    }
}
```

**Two-Layer Strategy:**
```
Level 1: Bloom Filter (in-memory, 12MB)
  └─> False positive? → Level 2: Redis (network call)
  
Benefits:
- 99% of lookups answered by Bloom filter (no network)
- 1% checked in Redis (eliminates false positives)
- Memory efficient: 12MB vs 640MB for hash set of 10M URLs
```

---

## 7. Distributed Locking

### Redlock Algorithm (Redis)

**Problem:** Multiple crawler instances shouldn't crawl same domain simultaneously

**Naive Approach (WRONG):**
```java
// DON'T DO THIS - race condition!
if (!redis.exists("lock:example.com")) {
    redis.set("lock:example.com", "worker-1");
    crawl("example.com");
}
```

**Atomic Solution:**
```java
public class DistributedLockService {
    
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
            owner,  // Unique worker ID
            ttlMs
        );
        
        return result != null && result == 1;
    }
    
    public boolean releaseLock(String resource, String owner) {
        String lockKey = "lock:" + resource;
        
        // Only owner can release (prevent accidental release)
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

**Usage Pattern:**
```java
String lockId = UUID.randomUUID().toString();
boolean acquired = lockService.acquireLock("domain:example.com", lockId, 30000);

if (acquired) {
    try {
        crawlDomain("example.com");
    } finally {
        lockService.releaseLock("domain:example.com", lockId);
    }
} else {
    log.info("Domain locked by another worker, skipping");
}
```

**Edge Cases:**
1. **Worker crashes while holding lock:** TTL expires automatically
2. **Network partition:** Lock times out, both workers may proceed (acceptable for crawling)
3. **Clock skew:** Use monotonic time for TTL

---

## 12. Production Optimizations

### Database Query Optimization

**Problem:** N+1 query anti-pattern

**Bad:**
```java
List<CrawlUrl> urls = crawlUrlRepository.findByStatus(PENDING);
for (CrawlUrl url : urls) {
    DomainMetadata meta = domainMetadataRepository.findByDomain(url.getDomain());
    // N+1: 1 query to get URLs + N queries for metadata
}
```

**Good:**
```java
@Query("SELECT cu FROM CrawlUrl cu " +
       "LEFT JOIN FETCH cu.domainMetadata " +
       "WHERE cu.status = :status")
List<CrawlUrl> findByStatusWithMetadata(@Param("status") CrawlStatus status);
// Single query with JOIN
```

### Elasticsearch Bulk Operations

```java
// Bad: Individual index requests
for (WebPage page : pages) {
    webPageRepository.save(page);  // N network calls
}

// Good: Bulk indexing
BulkRequest bulkRequest = new BulkRequest();
for (WebPage page : pages) {
    IndexRequest request = new IndexRequest("web_pages")
        .id(page.getUrl())
        .source(convertToJson(page));
    bulkRequest.add(request);
}
elasticsearchClient.bulk(bulkRequest);  // 1 network call
```

### Kafka Producer Tuning

```yaml
spring:
  kafka:
    producer:
      batch-size: 32768  # 32KB batches
      linger-ms: 10      # Wait 10ms to batch messages
      compression-type: snappy  # Compress batches
      acks: 1            # Leader acknowledgment (vs 'all' for durability)
```

**Trade-offs:**
- `acks=1`: Faster (leader ack only) vs `acks=all` (all replicas, slower but durable)
- `linger-ms=10`: Batch more messages vs `0` (send immediately)

---

*This is the deep, algorithm-focused guide you need! Focus on understanding the math, the distributed systems patterns, and the production considerations.*
