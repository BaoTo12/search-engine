# Content Deduplication with SimHash + LSH

> **Difficulty:** Advanced  
> **File:** [ContentDeduplicationService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/ContentDeduplicationService.java)  
> **Algorithm:** SimHash with Locality-Sensitive Hashing (LSH)

---

## 🎯 Problem Statement

**Challenge:** Detect near-duplicate web pages efficiently

**Real-World Examples:**
- **Mirror sites:** `example.com` and `example.org` serve identical content
- **Plagiarism:** Blog posts copied across multiple sites
- **Templates:** Pages differ only in header/footer/ads (90% identical content)
- **Quotes:** News sites republishing same article with minor edits

**Why Not String Comparison?**
- **Exact match only:** Misses 90% similar content
- **O(N²) complexity:** Compare every page pair = 500 billion comparisons for 1M pages
- **Memory:** Storing full text = GBs of RAM

---

## 🧮 SimHash Algorithm

### Core Idea

**Convert variable-length text → fixed-size 64-bit fingerprint**

**Property:** Similar documents produce similar fingerprints (low Hamming distance)

### Mathematical Foundation

```
Hamming Distance = number of bit positions where two hashes differ

Example:
hash1 = 1010110101...  (64 bits)
hash2 = 1011110101...  (64 bits)
          ^
Hamming distance = 1

Threshold: distance ≤ 3 → ~95% content similarity
```

### Algorithm Steps

1. **Tokenization:** Extract features (n-grams / shingles)
2. **Hashing:** Hash each feature to 64-bit integer
3. **Weighted Vector:** Build vector where each bit position accumulates weights
4. **Fingerprint:** Set bit to 1 if vector[i] > 0, else 0

### Detailed Implementation

```java
public long calculateSimHash(String content) {
    // Step 1: Extract features (5-word shingles)
    List<String> features = extractShingles(content, 5);
    
    // Step 2: Initialize 64-dimensional vector
    int[] vector = new int[64];
    
    // Step 3: Process each feature
    for (String feature : features) {
        // Hash feature to 64-bit using MurmurHash3
        long hash = MurmurHash3.hash64(feature.getBytes(StandardCharsets.UTF_8));
        
        // Weight by term frequency (TF)
        int weight = Collections.frequency(features, feature);
        
        // Update vector: +weight if bit=1, -weight if bit=0
        for (int bitPos = 0; bitPos < 64; bitPos++) {
            boolean bitIsSet = ((hash >> bitPos) & 1L) == 1;
            
            if (bitIsSet) {
                vector[bitPos] += weight;
            } else {
                vector[bitPos] -= weight;
            }
        }
    }
    
    // Step 4: Generate final fingerprint
    long fingerprint = 0L;
    for (int i = 0; i < 64; i++) {
        if (vector[i] > 0) {
            fingerprint |= (1L << i);  // Set bit i to 1
        }
    }
    
    return fingerprint;
}

private List<String> extractShingles(String content, int n) {
    String normalized = content.toLowerCase()
                              .replaceAll("[^a-z0-9\\s]", "")  // Remove punctuation
                              .replaceAll("\\s+", " ");         // Normalize whitespace
    
    String[] words = normalized.split(" ");
    List<String> shingles = new ArrayList<>();
    
    // Create n-grams (sliding window of n words)
    for (int i = 0; i <= words.length - n; i++) {
        String shingle = String.join(" ", Arrays.copyOfRange(words, i, i + n));
        shingles.add(shingle);
    }
    
    return shingles;
}
```

### Step-by-Step Example

**Input Text:**
```
"The quick brown fox jumps over the lazy dog"
```

**Step 1: Extract 3-word shingles**
```
["the quick brown", "quick brown fox", "brown fox jumps", 
 "fox jumps over", "jumps over the", "over the lazy", "the lazy dog"]
```

**Step 2 & 3: Hash and build vector**
```
Shingle "the quick brown" → hash = 0101101001... (64 bits)
Weight = 1 (appears once)

For each bit position:
- If bit=1: vector[pos] += 1
- If bit=0: vector[pos] -= 1

After all shingles:
vector = [3, -1, 5, 2, -4, ..., 1]  (64 values)
```

**Step 4: Generate fingerprint**
```
If vector[i] > 0: set bit i = 1
Else: set bit i = 0

Fingerprint = 1011001010... (64 bits)
```

### Similarity Detection

```java
public boolean isDuplicate(long hash1, long hash2, int threshold) {
    // XOR finds bit differences
    long xor = hash1 ^ hash2;
    
    // Count number of 1 bits (Hamming distance)
    int hammingDistance = Long.bitCount(xor);
    
    // threshold=3 means ≤3 bits different ≈ 95% similar
    return hammingDistance <= threshold;
}
```

**Why This Works:**
```
Similar content → Similar feature sets → Similar hash bits
XOR operation: O(1) comparison vs O(N) string comparison!

Examples:
- Identical documents: Hamming distance = 0
- 95% similar: Hamming distance ≤ 3
- 50% similar: Hamming distance ≈ 32
- Random documents: Hamming distance ≈ 32 (expected)
```

---

## 🚀 Locality-Sensitive Hashing (LSH)

### The Scalability Problem

**Naive Approach:**
```
For 1M pages:
- Compare each page to every other page
- Comparisons = C(1M, 2) = 500 billion
- Even at 1µs per comparison = 500,000 seconds ≈ 6 days
```

**Solution:** LSH reduces candidate set

### LSH Index Structure

```java
public class LSHIndex {
    // Split 64-bit fingerprint into 4 bands × 16 bits
    // If ANY band matches → candidate for full comparison
    
    private static final int NUM_BANDS = 4;
    private static final int BITS_PER_BAND = 16;
    
    // band_id → Set<URL>
    private Map<Integer, Set<String>> buckets = new ConcurrentHashMap<>();
    
    public void index(String url, long simhash) {
        // Extract 4 × 16-bit segments
        for (int band = 0; band < NUM_BANDS; band++) {
            // Extract bits [band*16, (band+1)*16)
            int segment = extractSegment(simhash, band);
            
            // Add URL to bucket for this segment
            buckets.computeIfAbsent(segment, k -> ConcurrentHashMap.newKeySet())
                   .add(url);
        }
    }
    
    private int extractSegment(long simhash, int bandIndex) {
        int shift = bandIndex * BITS_PER_BAND;
        long mask = (1L << BITS_PER_BAND) - 1;  // 16 bits of 1s
        return (int)((simhash >> shift) & mask);
    }
    
    public Set<String> findCandidates(long querySimhash) {
        Set<String> candidates = new HashSet<>();
        
        // Check all bands
        for (int band = 0; band < NUM_BANDS; band++) {
            int segment = extractSegment(querySimhash, band);
            
            if (buckets.containsKey(segment)) {
                candidates.addAll(buckets.get(segment));
            }
        }
        
        return candidates;  // Then verify with Hamming distance
    }
}
```

### LSH Performance

**Example: 1M pages, average 100 pages per bucket**

```
Without LSH:
- Comparisons: 1,000,000
- Time: O(N)

With LSH:
- Candidate retrieval: O(1) hash lookup × 4 bands
- Candidates: ~400 pages (4 bands × 100 avg bucket size)
- Comparisons: 400
- Speedup: 2,500x!
```

**False Negative Rate:**
```
Probability that similar pages hash to different buckets in ALL bands

P(miss) = (1 - similarity^BITS_PER_BAND)^NUM_BANDS

For 95% similar (Hamming distance 3):
P(miss) ≈ (1 - 0.95^16)^4 ≈ 10^-10  (extremely low!)
```

---

## 🔍 Full Deduplication Pipeline

```java
@Service
public class ContentDeduplicationService {
    
    private final LSHIndex lshIndex;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final int HAMMING_THRESHOLD = 3;
    
    @Transactional
    public boolean isDuplicate(String url, String content) {
        // 1. Calculate SimHash
        long simhash = calculateSimHash(content);
        
        // 2. Find candidates using LSH
        Set<String> candidates = lshIndex.findCandidates(simhash);
        
        // 3. Verify each candidate with exact Hamming distance
        for (String candidateUrl : candidates) {
            Long candidateHash = getStoredHash(candidateUrl);
            
            if (candidateHash != null) {
                int distance = Long.bitCount(simhash ^ candidateHash);
                
                if (distance <= HAMMING_THRESHOLD) {
                    log.info("Duplicate found: {} similar to {}", url, candidateUrl);
                    return true;  // Duplicate detected
                }
            }
        }
        
        // 4. Not a duplicate - store for future comparisons
        storeHash(url, simhash);
        lshIndex.index(url, simhash);
        
        return false;
    }
    
    private void storeHash(String url, long simhash) {
        String key = "simhash:" + hashUrl(url);
        redisTemplate.opsForValue().set(key, String.valueOf(simhash), 30, TimeUnit.DAYS);
    }
    
    private Long getStoredHash(String url) {
        String key = "simhash:" + hashUrl(url);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }
}
```

---

## 📊 Performance Analysis

### Memory Usage

```
Per Page:
- SimHash: 8 bytes (64-bit long)
- LSH entry: 4 × pointer ≈ 32 bytes
Total: ~40 bytes per page

For 10M pages:
- Total memory: 400 MB
- vs full text: ~50 GB (average 5KB per page)
- Savings: 125x
```

### Time Complexity

| Operation | Without LSH | With LSH |
|-----------|-------------|----------|
| **Index page** | O(1) | O(1) |
| **Find duplicates** | O(N) | O(k) where k << N |
| **Comparison** | O(text_length) | O(1) XOR |

**For 1M pages:**
- Traditional: 1M string comparisons ≈ 10 minutes
- SimHash + LSH: 400 comparisons ≈ 4ms
- **Speedup: 150,000x**

---

## 🎯 Use Cases in Search Engine

### 1. Skip Duplicate Indexing

```java
@KafkaListener(topics = "pages")
public void indexPage(WebPageDTO page) {
    // Check for duplicates before indexing
    if (deduplicationService.isDuplicate(page.getUrl(), page.getContent())) {
        log.info("Skipping duplicate page: {}", page.getUrl());
        
        // Optionally: Track canonical URL
        markAsCanonical(findCanonicalUrl(page));
        return;
    }
    
    // Index unique content
    elasticsearchService.index(page);
}
```

### 2. Deduplicate Search Results

```java
public List<SearchResult> deduplicateResults(List<SearchResult> results) {
    Set<Long> seenHashes = new HashSet<>();
    List<SearchResult> unique = new ArrayList<>();
    
    for (SearchResult result : results) {
        long hash = calculateSimHash(result.getContent());
        
        boolean isDuplicate = seenHashes.stream()
            .anyMatch(h -> Long.bitCount(hash ^ h) <= 3);
        
        if (!isDuplicate) {
            seenHashes.add(hash);
            unique.add(result);
        }
    }
    
    return unique;
}
```

### 3. Find Similar Pages

```java
public List<String> findSimilarPages(String url, int limit) {
    Long hash = getStoredHash(url);
    if (hash == null) return Collections.emptyList();
    
    Set<String> candidates = lshIndex.findCandidates(hash);
    
    return candidates.stream()
        .map(candidateUrl -> {
            Long candidateHash = getStoredHash(candidateUrl);
            int distance = Long.bitCount(hash ^ candidateHash);
            return new SimilarityResult(candidateUrl, 1.0 - distance/64.0);
        })
        .filter(r -> r.similarity > 0.85)
        .sorted(Comparator.comparing(SimilarityResult::similarity).reversed())
        .limit(limit)
        .map(SimilarityResult::url)
        .collect(Collectors.toList());
}
```

---

## 🔬 Advanced: TF-IDF Weighting

**Problem:** All features weighted equally → common words dominate

**Solution:** Weight by TF-IDF (Term Frequency × Inverse Document Frequency)

```java
private Map<String, Double> calculateTFIDF(List<String> features, long totalDocs) {
    Map<String, Integer> termFreq = new HashMap<>();
    Map<String, Double> tfidf = new HashMap<>();
    
    // Calculate TF
    for (String feature : features) {
        termFreq.merge(feature, 1, Integer::sum);
    }
    
    // Calculate TF-IDF
    for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
        String term = entry.getKey();
        int tf = entry.getValue();
        
        // IDF from global statistics (stored in Redis/DB)
        long docFreq = getDocumentFrequency(term);
        double idf = Math.log((double) totalDocs / (docFreq + 1));
        
        tfidf.put(term, tf * idf);
    }
    
    return tfidf;
}

public long calculateSimHashWithTFIDF(String content) {
    List<String> features = extractShingles(content, 5);
    Map<String, Double> weights = calculateTFIDF(features, getTotalDocCount());
    
    int[] vector = new int[64];
    
    for (String feature : features) {
        long hash = MurmurHash3.hash64(feature.getBytes());
        int weight = (int)(weights.get(feature) * 100);  // Scale to integer
        
        for (int i = 0; i < 64; i++) {
            vector[i] += ((hash >> i) & 1) == 1 ? weight : -weight;
        }
    }
    
    long fingerprint = 0L;
    for (int i = 0; i < 64; i++) {
        if (vector[i] > 0) fingerprint |= (1L << i);
    }
    
    return fingerprint;
}
```

---

*SimHash + LSH provides O(1) near-duplicate detection at scale - essential for production search engines!*
