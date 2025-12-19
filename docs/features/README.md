# Feature Documentation Index

This directory contains detailed deep-dive documentation for each special feature of the search engine.

## 📚 Available Documentation

### 1. ✅ PageRank Algorithm
**File:** [pagerank-algorithm.md](pagerank-algorithm.md)  
**Topics:** Graph algorithm, power iteration, convergence, optimization

### 2. ✅ SimHash Content Deduplication  
**File:** [content-deduplication-simhash.md](content-deduplication-simhash.md)  
**Topics:** 64-bit fingerprints, Hamming distance, LSH bucketing

### 3. ✅ Bloom Filters
**File:** [bloom-filters.md](bloom-filters.md)  
**Topics:** Probabilistic data structures, false positive rates, two-layer verification

### 4. URL Prioritization Strategies
**File:** [url-prioritization-strategies.md](url-prioritization-strategies.md)  
**Topics:** BFS, Best-First, OPIC, Focused Crawling

### 5. Distributed Crawling with Kafka
**File:** [distributed-crawling-kafka.md](distributed-crawling-kafka.md)  
**Topics:** Partitioning, consistency, consumer groups

### 6. Query Expansion & NLP
**File:** [query-expansion-nlp.md](query-expansion-nlp.md)  
**Topics:** Spell correction, synonyms, entity detection

### 7. Token Bucket Rate Limiter
**File:** [rate-limiting-token-bucket.md](rate-limiting-token-bucket.md)  
**Topics:** Distributed rate limiting, Redis Lua scripts

### 8. Circuit Breaker Pattern
**File:** [circuit-breaker.md](circuit-breaker.md)  
**Topics:** Fault tolerance, state machines, auto-recovery

### 9. Distributed Locking (Redlock)
**File:** [distributed-locking-redlock.md](distributed-locking-redlock.md)  
**Topics:** Redis distributed locks, atomic operations

### 10. Robots.txt Compliance
**File:** [robots-txt-compliance.md](robots-txt-compliance.md)  
**Topics:** RFC 9309, wildcard matching, sitemap parsing

---

## 📖 Reading Guide

**For Beginners:** Start with Bloom Filters → Circuit Breaker → Rate Limiting

**For Advanced:** PageRank → SimHash → Distributed Crawling

**For Distributed Systems:** Kafka Partitioning → Distributed Locking  → Circuit Breaker

---

*Each document includes mathematical foundations, implementation details, and production optimizations.*
