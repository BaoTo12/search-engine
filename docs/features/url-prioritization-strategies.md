# URL Prioritization Strategies

> **File:** [URLPrioritizationStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/URLPrioritizationStrategy.java)  
> **Pattern:** Strategy Pattern for pluggable algorithms

---

## Strategy Pattern Implementation

### Interface

```java
public interface URLPrioritizationStrategy {
    double calculatePriority(String url, int depth, Double pageRank, String domain);
    String getStrategyName();
}
```

---

## 1. BFS (Breadth-First Search)

**File:** [BFSStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/BFSStrategy.java)

### Algorithm
Crawl web level-by-level from seed URLs.

```
Depth 0: [seed1, seed2, seed3]
         ↓
Depth 1: [all links from depth 0]
         ↓
Depth 2: [all links from depth 1]
```

### Implementation

```java
@Component("BFS")
public class BFSStrategy implements URLPrioritizationStrategy {
    
    @Override
    public double calculatePriority(String url, int depth, Double pageRank, String domain) {
        // Lower depth = higher priority
        // depth 0: 100, depth 1: 90, depth 2: 80...
        return Math.max(0, 100 - (depth * 10));
    }
}
```

**Use Case:** General web crawling, discover new content quickly

**Pros:**
- Simple, predictable
- Ensures broad coverage
- Good for finding popular pages early

**Cons:**
- No quality filtering
- May crawl low-value pages

---

## 2. Best-First Search

**File:** [BestFirstStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/BestFirstStrategy.java)

### Algorithm
Always crawl the highest-quality URL next.

### Priority Formula

```
Priority = (PageRank × 50) + (DomainAuthority × 30) + (DepthBonus × 20)

Where:
- PageRank: 0-1 (from previous calculation)
- DomainAuthority: Quality score 0-1
- DepthBonus: (MAX_DEPTH - depth) / MAX_DEPTH
```

### Implementation

```java
@Component("BEST_FIRST")
public class BestFirstStrategy implements URLPrioritizationStrategy {
    
    @Autowired
    private DomainMetadataRepository domainRepository;
    
    private static final int MAX_DEPTH = 10;
    
    @Override
    public double calculatePriority(String url, int depth, Double pageRank, String domain) {
        // 1. PageRank component (0-50 points)
        double prScore = (pageRank != null ? pageRank : 0.5) * 50;
        
        // 2. Domain authority (0-30 points)
        double domainScore = getDomainAuthority(domain) * 30;
        
        // 3. Depth bonus (0-20 points)
        double depthScore = ((double)(MAX_DEPTH - depth) / MAX_DEPTH) * 20;
        
        return prScore + domainScore + depthScore;
    }
    
    private double getDomainAuthority(String domain) {
        return domainRepository.findByDomain(domain)
            .map(DomainMetadata::getAuthorityScore)
            .orElse(0.5);  // Default for unknown domains
    }
}
```

**Domain Authority Calculation:**

```java
public double calculateDomainAuthority(String domain) {
    DomainMetadata metadata = domainRepository.findByDomain(domain);
    
    // Metrics:
    double successRate = metadata.getSuccessfulCrawls() / metadata.getTotalCrawls();
    double avgPageRank = metadata.getAveragePageRank();
    int inboundDomains = metadata.getInboundDomainCount();
    
    // Weighted average
    return (successRate * 0.3) + (avgPageRank * 0.5) + 
           (Math.min(inboundDomains / 100.0, 1.0) * 0.2);
}
```

**Use Case:** Limited crawl budget, need high-quality content

**Pros:**
- Efficient resource usage
- Prioritizes authoritative content
- Good quality/coverage trade-off

**Cons:**
- May miss niche but valuable content
- Depends on accurate PageRank

---

## 3. OPIC (Online Page Importance Computation)

**File:** [OPICStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/OPICStrategy.java)

### Algorithm
"Cash distribution" model - pages transfer importance to links.

### Cash Flow Model

```
1. Each page starts with $1.00 cash
2. When crawled:
   - Split cash evenly among outbound links
   - Transfer to those URLs
3. Crawl pages with most accumulated cash first

Total cash conserved: Σ(cash) = N × $1.00
```

### Implementation

```java
@Component("OPIC")
public class OPICStrategy implements URLPrioritizationStrategy {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String CASH_PREFIX = "opic:cash:";
    private static final double INITIAL_CASH = 1.0;
    
    @Override
    public double calculatePriority(String url, int depth, Double pageRank, String domain) {
        String cashKey = CASH_PREFIX + hashUrl(url);
        String cashValue = redisTemplate.opsForValue().get(cashKey);
        return cashValue != null ? Double.parseDouble(cashValue) : INITIAL_CASH;
    }
    
    // Called after page is crawled
    public void distributeCash(String sourceUrl, List<String> outboundLinks) {
        if (outboundLinks.isEmpty()) return;
        
        // Get source cash
        String sourceKey = CASH_PREFIX + hashUrl(sourceUrl);
        double sourceCash = getCash(sourceKey);
        
        // Distribute evenly
        double cashPerLink = sourceCash / outboundLinks.size();
        
        // Atomic increment using Lua
        String luaScript =
            "local current = tonumber(redis.call('get', KEYS[1]) or '0') " +
            "local newVal = current + tonumber(ARGV[1]) " +
            "redis.call('set', KEYS[1], tostring(newVal)) " +
            "redis.call('expire', KEYS[1], 86400) " +
            "return newVal";
        
        for (String targetUrl : outboundLinks) {
            String targetKey = CASH_PREFIX + hashUrl(targetUrl);
            redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Double.class),
                Collections.singletonList(targetKey),
                String.valueOf(cashPerLink)
            );
        }
        
        // Source spent all cash
        redisTemplate.opsForValue().set(sourceKey, "0");
    }
}
```

**Mathematical Properties:**

```
∀t: Σ cash(page_i, t) = N  (conservation law)

Steady-state ≈ PageRank without global iteration!
```

**Use Case:** Real-time crawling, no batch PageRank computation

**Pros:**
- O(1) priority calculation
- No global graph analysis
- Approximates PageRank

**Cons:**
- Less accurate than full PageRank
- Requires stateful tracking

---

## 4. Focused Crawling

**File:** [FocusedCrawlingStrategy.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/strategy/FocusedCrawlingStrategy.java)

### Algorithm
Target specific topics or domains.

### Configuration

```java
@Data
@Builder
public class FocusedCrawlingConfig {
    private Set<String> allowedDomains;
    private Set<String> topicKeywords;
    private double relevanceThreshold;  // 0.0 - 1.0
    private boolean strictMode;
}
```

### Implementation

```java
@Component("FOCUSED")
public class FocusedCrawlingStrategy implements URLPrioritizationStrategy {
    
    private FocusedCrawlingConfig config;
    
    @Override
    public double calculatePriority(String url, int depth, Double pageRank, String domain) {
        // 1. Domain whitelist check
        if (!isAllowedDomain(domain)) {
            return 0;  // Skip entirely
        }
        
        // 2. URL relevance analysis
        double relevance = calculateRelevance(url);
        
        // 3. Apply threshold
        if (relevance < config.getRelevanceThreshold()) {
            return 0;
        }
        
        // 4. Boost by PageRank if available
        double prBoost = pageRank != null ? pageRank * 50 : 25;
        
        return (relevance * 50) + prBoost;
    }
    
    private boolean isAllowedDomain(String domain) {
        if (config.getAllowedDomains().isEmpty()) {
            return true;  // No restriction
        }
        return config.getAllowedDomains().stream()
            .anyMatch(allowed -> domain.contains(allowed));
    }
    
    private double calculateRelevance(String url) {
        String urlLower = url.toLowerCase();
        
        // Count keyword matches in URL
        long matches = config.getTopicKeywords().stream()
            .filter(keyword -> urlLower.contains(keyword.toLowerCase()))
            .count();
        
        return (double) matches / config.getTopicKeywords().size();
    }
}
```

### Example Configuration

```java
// Java programming search engine
FocusedCrawlingConfig javaConfig = FocusedCrawlingConfig.builder()
    .allowedDomains(Set.of(
        "stackoverflow.com",
        "docs.oracle.com",
        "spring.io",
        "baeldung.com"
    ))
    .topicKeywords(Set.of(
        "java", "spring", "jvm", "maven",
        "gradle", "hibernate", "junit"
    ))
    .relevanceThreshold(0.3)  // At least 30% keyword match
    .build();
```

**Use Case:** Vertical/niche search engines

**Pros:**
- Highly efficient (no wasted crawls)
- Domain-specific coverage
- Predictable results

**Cons:**
- May miss related content
- Requires domain knowledge

---

## Strategy Selection Guide

| Scenario | Best Strategy | Why |
|----------|---------------|-----|
| **General web** | BFS | Broad coverage |
| **Limited budget** | Best-First | Quality over quantity |
| **Real-time crawling** | OPIC | No batch computation |
| **Topic-specific** | Focused | Targeted efficiency |
| **News aggregation** | Best-First + Recency | Fresh + quality |

---

## Switching Strategies at Runtime

```java
@Service
public class URLFrontierService {
    
    private final Map<String, URLPrioritizationStrategy> strategies;
    private URLPrioritizationStrategy currentStrategy;
    
    @Autowired
    public URLFrontierService(List<URLPrioritizationStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                URLPrioritizationStrategy::getStrategyName,
                Function.identity()
            ));
        this.currentStrategy = strategies.get("BFS");  // Default
    }
    
    public void setStrategy(String strategyName) {
        URLPrioritizationStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        }
        this.currentStrategy = strategy;
        log.info("Switched to strategy: {}", strategyName);
    }
    
    public double prioritize(CrawlUrl url) {
        return currentStrategy.calculatePriority(
            url.getUrl(),
            url.getDepth(),
            url.getPageRank(),
            url.getDomain()
        );
    }
}
```

**Admin API:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/frontier/strategy \
  -H "Content-Type: application/json" \
  -d '{"strategy":"BEST_FIRST"}'
```

---

*Pluggable strategies enable flexible crawling based on goals and resources!*
