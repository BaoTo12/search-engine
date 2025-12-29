# 🕸️ Distributed Crawler & Indexer (Search Engine Core)
🧭 Overview

This project aims to build a complete, scalable search engine with a **Google-like web interface**.
It consists of multiple backend services (microservices) that coordinate via Kafka and store structured data in Elasticsearch, plus a modern **Next.js frontend** that provides an intuitive search experience similar to Google.

🎯 Goals
- Crawl and index web content from multiple domains efficiently.
- Scale horizontally (add more crawlers easily).
- Store, tokenize, and rank indexed documents.
- Provide search results through a REST API.


🧱 System Architecture Overview
```
+-------------------------------------------------------------+
|              User Search Interface (Web UI)                 |
|          Next.js Frontend (Google-like Interface)           |
+-----------------------------+-------------------------------+
                              |
                              v
                 +----------------------------+
                 |    Search API (REST)       |
                 +----------------------------+
                              |
                              v
                   +----------------------+
                   |   Search Service     | ---> Query Elasticsearch
                   +----------------------+
                              ^
                              |
                  +------------------------+
                  |   Indexing Service     |
                  +------------------------+
                              ^
                              |
                  +------------------------+
                  |   Kafka Topic: pages   |
                  +------------------------+
                              ^
                              |
     +------------------------------------------------------+
     |                Crawler Workers (multiple)            |
     |------------------------------------------------------|
     | Fetch URL | Parse HTML | Extract Links | Push Content|
     +------------------------------------------------------+
                              ^
                              |
                   +------------------------+
                   |   Crawl Scheduler      |
                   |  (URL frontier manager)|
                   +------------------------+
                              ^
                              |
                   +------------------------+
                   |   Seed URLs Database   |
                   +------------------------+
```
## ⚙️ Core Components

### 1. 🧩 Crawl Scheduler (Dispatcher)
**Purpose:** Manages the list of URLs to crawl (“frontier”) and dispatches them to workers via Kafka.

**Responsibilities:**
- Maintain a queue of pending URLs.  
- Deduplicate URLs (avoid revisits unless expired).  
- Distribute URLs in batches to Kafka topic `crawl-requests`.  
- Monitor crawl frequency per domain (rate limiting).  
- Support crawl priorities (e.g., news sites > blogs).  

**Inputs:**  
- Seed URLs (manually added or from previous crawl results).  

**Outputs:**  
- Messages to Kafka topic `crawl-requests` containing `{ url, depth, domain, timestamp }`.

---

### 2. 🕷️ Crawler Worker (Fetcher + Parser)
**Purpose:** Executes crawling tasks concurrently, processing messages from Kafka.

**Responsibilities:**
- Consume URLs from Kafka (`crawl-requests` topic).  
- Fetch page HTML using HTTP clients with retry and timeout.  
- Parse HTML (JSoup or similar).  
- Extract metadata: title, description, text content, outbound links.  
- Push cleaned text to Kafka topic `pages` for indexing.  
- Send discovered links back to the scheduler (`new-links` topic).

**Concurrency Design:**
- Use Java’s `ThreadPoolExecutor` for parallel fetching.  
- Each worker handles 20–100 threads depending on I/O capacity.  
- Retry logic with exponential backoff for failed fetches.

**Error Handling:**
- HTTP 429 / 5xx → retry later.  
- Invalid or duplicate URLs → ignored.

---

### 3. 🔗 Link Extractor / URL Frontier Manager
**Purpose:** Processes newly discovered URLs, validates and normalizes them before adding to the crawl queue.

**Responsibilities:**
- Validate URLs (avoid non-HTML or disallowed domains).  
- Normalize URLs (remove tracking params, resolve relative paths).  
- Deduplicate using Redis (set of known URLs).  
- Enqueue unique URLs for future crawling.  

**Persistence:**
- Redis or PostgreSQL tables for:
  - `visited_urls`
  - `pending_urls`
  - `domain_rate_limits`

---

## 🧮 Web Crawling Algorithms

### 1. 📊 URL Frontier Management Algorithms

#### **Breadth-First Search (BFS)** 🌊
- **Purpose**: Crawl web pages level by level from seed URLs
- **How it works**: 
  - Start with seed URLs (depth 0)
  - Crawl all links from those pages (depth 1)
  - Then crawl all links from depth 1 pages (depth 2)
  - Continue until max depth reached
- **Pros**: Finds important pages quickly, good coverage
- **Cons**: Can get stuck on large sites
- **Use case**: General-purpose web crawling

#### **Best-First Search** 🎯
- **Purpose**: Prioritize crawling high-value pages first
- **How it works**: 
  - Assign priority scores to URLs (based on PageRank, domain authority, etc.)
  - Always crawl the highest-priority URL next
  - Update priorities as new links are discovered
- **Pros**: Maximizes crawl efficiency, finds important content faster
- **Cons**: May miss less popular but valuable content
- **Use case**: Limited crawl budget, focused crawling

#### **Focused Crawling** 🔍
- **Purpose**: Target specific topics or domains
- **How it works**:
  - Use classifiers to predict page relevance
  - Only follow links likely to lead to target content
  - Adjust strategy based on hit rate
- **Pros**: Very efficient for topic-specific crawling
- **Cons**: May miss related content
- **Use case**: Vertical search engines, topic-specific indexes

---

### 2. ⚖️ URL Prioritization Algorithms

#### **PageRank-based Prioritization** 📈
- **Formula**: Priority = PageRank(page) × Freshness(page)
- **Implementation**:
  ```
  score = (inbound_links_count × link_quality) / time_since_last_crawl
  ```
- **Use case**: Prioritize authoritative pages

#### **OPIC (Online Page Importance Computation)** 💰
- **Purpose**: Calculate page importance without full graph analysis
- **How it works**:
  - Each page starts with cash = 1.0
  - When crawled, splits cash among outbound links
  - Pages with more incoming "cash" crawled first
- **Pros**: Lightweight, no global computation needed
- **Cons**: Less accurate than full PageRank
- **Use case**: Real-time crawl prioritization

#### **Domain Authority Scoring** 🏆
- **Purpose**: Prioritize pages from high-quality domains
- **Metrics**:
  - Domain age and reputation
  - Historical content quality
  - Backlink profile
  - Crawl success rate
- **Use case**: Balancing breadth and quality

---

### 3. 🔒 URL Deduplication Algorithms

#### **Bloom Filter** 🌸
- **Purpose**: Fast, memory-efficient duplicate detection
- **How it works**:
  - Hash URLs multiple times with different functions
  - Set bits in bit array
  - Check if all bits set before crawling
- **Pros**: Very fast (O(1)), low memory usage
- **Cons**: False positives possible (but no false negatives)
- **Space**: ~10 bits per URL for 1% error rate
- **Use case**: First-level deduplication

#### **Hash-based Deduplication** #️⃣
- **Implementation**:
  ```java
  String urlHash = SHA256(normalizedUrl);
  if (redisCache.exists(urlHash)) {
      return DUPLICATE;
  }
  redisCache.set(urlHash, true, TTL);
  ```
- **Pros**: 100% accurate, persistent
- **Cons**: Higher memory/storage cost
- **Use case**: Critical deduplication, permanent storage

#### **URL Normalization** 🔧
- **Techniques**:
  - Convert to lowercase: `HTTP://Example.Com` → `http://example.com`
  - Remove default ports: `http://site.com:80` → `http://site.com`
  - Sort query parameters: `?b=2&a=1` → `?a=1&b=2`
  - Remove tracking params: `?utm_source=...` → (removed)
  - Resolve relative paths: `/./page` → `/page`
  - Add trailing slash to directories: `/dir` → `/dir/`

---

### 4. 🤝 Politeness & Rate Limiting Algorithms

#### **Token Bucket Algorithm** 🪣
- **Purpose**: Allow burst traffic while limiting average rate
- **How it works**:
  ```
  bucket_capacity = 10 requests
  refill_rate = 1 request/second
  
  if bucket.tokens > 0:
      bucket.tokens -= 1
      crawl(url)
  else:
      wait(until token available)
  ```
- **Pros**: Flexible, allows controlled bursts
- **Cons**: More complex than fixed-rate limiting
- **Use case**: Per-domain rate limiting

#### **Exponential Backoff** ⏱️
- **Purpose**: Gracefully handle errors and rate limits
- **Implementation**:
  ```
  wait_time = min(base_delay × 2^(attempt_count), max_delay)
  wait_time += random(0, wait_time × 0.1)  // jitter
  ```
- **Use case**: Retry logic after 429/503 errors

#### **Domain-based Scheduling** 🌐
- **Purpose**: Respect crawl-delay from robots.txt
- **How it works**:
  - Maintain per-domain queues
  - Track last crawl time per domain
  - Enforce minimum delay between requests
  ```
  min_delay = max(
      robots_txt.crawl_delay,
      default_politeness_delay,
      adaptive_delay_based_on_errors
  )
  ```

---

### 5. 🔄 Recrawl & Freshness Algorithms

#### **Freshness-based Scheduling** 🕐
- **Purpose**: Keep index up-to-date
- **Strategies**:
  - **Uniform**: Recrawl all pages at same frequency
  - **Proportional**: More frequent recrawl for fast-changing pages
  - **Poisson Process**: Model page changes probabilistically
  
#### **Change Detection** 🔎
- **Techniques**:
  - **Hash Comparison**: Compare content hash with previous crawl
  - **Last-Modified Header**: Use HTTP headers
  - **ETag**: Efficient change detection
  - **Sampling**: Predict change frequency from history

#### **Adaptive Recrawl** 🎛️
- **Formula**:
  ```
  recrawl_frequency = base_frequency × 
                      page_importance_score × 
                      historical_change_rate
  ```

---

### 6. 🌍 Distributed Crawling Algorithms

#### **Consistent Hashing** 🔑
- **Purpose**: Distribute URLs across crawler nodes
- **How it works**:
  ```
  node = hash(domain) % num_crawler_nodes
  ```
- **Pros**: Even distribution, easy to scale
- **Cons**: Domain locality may cause imbalance

#### **Hash-based Partitioning** 📦
- **Purpose**: Ensure same domain goes to same crawler (politeness)
- **Implementation**:
  ```
  partition = hash(domain) % kafka_partitions
  ```
- **Benefit**: Single consumer per domain = automatic rate limiting

#### **Work Stealing** 🏃
- **Purpose**: Balance load dynamically
- **How it works**:
  - Idle crawlers steal work from busy crawlers
  - Maintains work queues per crawler
  - Periodic load balancing

---

### 7. 🎯 Content Quality & Selection

#### **Content Fingerprinting (SimHash)** 🔢
- **Purpose**: Detect near-duplicate content
- **How it works**:
  1. Tokenize content into features
  2. Hash each feature
  3. Weight features by frequency (TF-IDF)
  4. Combine into 64-bit fingerprint
  5. Compare fingerprints (Hamming distance < 3 = duplicate)
- **Use case**: Deduplication of mirror sites, scraped content

#### **Link Quality Scoring** ⭐
- **Metrics**:
  - Anchor text relevance
  - Position in page (header links > footer links)
  - Context similarity
  - URL structure (depth, parameters)
- **Use for**: Prioritizing which links to follow

---

## 📋 Algorithm Selection Guide

| Scenario | Recommended Algorithms |
|----------|------------------------|
| **General web crawling** | BFS + PageRank prioritization + Bloom filter |
| **Topic-specific crawling** | Focused crawling + Best-first search |
| **Limited resources** | Best-first + OPIC + Aggressive deduplication |
| **Fresh content (news)** | Adaptive recrawl + Freshness-based scheduling |
| **Large scale (billions of pages)** | Distributed + Consistent hashing + Bloom filters |
| **Respectful crawling** | Token bucket + Exponential backoff + robots.txt |

---

### 4. 📥 Indexer Service
**Purpose:** Converts raw page data into an inverted index for fast search queries.

**Responsibilities:**
- Consume from Kafka topic `pages`.  
- Tokenize text content (stop word removal, stemming).  
- Extract metadata: URL, title, keywords, frequency counts.  
- Store in Elasticsearch or Lucene.  
- Update entries if pages change over time.  

**Data Model Example:**
```json
{
  "url": "https://example.com/article",
  "title": "Example Article",
  "content": "Cleaned text content ...",
  "tokens": ["example", "article", "web"],
  "links": ["https://another.com"],
  "last_crawled": "2025-10-29T13:00:00Z"
}
```
Ranking Features:
TF-IDF weighting.
PageRank-style scoring (based on inbound links).
Optional: semantic relevance model.

### 5. 🔍 Search API Service

Purpose: Exposes endpoints to perform user queries against the indexed data.

Responsibilities:

Handle queries like /search?q=java+concurrency&page=1.

Translate to Elasticsearch syntax.

Rank, paginate, and format results.

Cache hot queries in Redis.

Log query metrics for analytics.

Example Response:
```
{
  "query": "java concurrency",
  "total_results": 2493,
  "results": [
    {
      "title": "Understanding Java Thread Pools",
      "url": "https://medium.com/java/thread-pools",
      "snippet": "... thread pools manage concurrency efficiently ..."
    }
  ]
}
```
## 🧩 Data Flow Summary
🔹 Crawl Phase

Scheduler sends seed URLs → Kafka crawl-requests.

Worker fetches HTML → extracts links & content.

Extracted links → Kafka new-links.

Cleaned content → Kafka pages.

🔹 Index Phase

Indexer consumes from pages → tokenizes → stores in Elasticsearch.

Optional: offline ranking updates.

🔹 Search Phase

User types query in **Web UI** → Search API → queries Elasticsearch.

Results returned → ranked → paginated → displayed in Google-like interface.

### 🚀 Scaling Strategy
Component	Scaling Approach
Scheduler	Stateless → horizontally scalable
Crawler Worker	Multiple replicas consuming Kafka partitions
Indexer	Scales via Kafka partitions and Elasticsearch shards
Search API	Stateless → load balanced
Redis	Central deduplication store
Kafka	Partition by domain or URL hash
Elasticsearch	Cluster with sharding and replication

Key Idea:
All communication happens asynchronously through Kafka topics, ensuring elasticity and fault isolation.

### 🧠 Runtime Behavior

Crawl Burst: Kafka backpressure controls message flow.

Network Failures: Workers retry with exponential backoff; failed URLs → dead-letter queue.

Duplicates: Redis deduplication and checksum hashing.

Re-crawling: Scheduler revisits URLs periodically.

Ranking: Offline analyzer recalculates scores weekly.

### 📊 Monitoring & Observability

Prometheus + Grafana: Crawl rate, error rates, queue sizes.

Elastic APM: Trace end-to-end latency (crawler → indexer → search).

Kafka Lag Exporter: Track consumer lag.

ELK Stack: Log aggregation and search.

### 🔒 Fault Tolerance
Issue	Mitigation
Worker crash	Kafka rebalances consumer groups
Scheduler down	Redis retains pending URLs
Indexer failure	Kafka offsets prevent data loss
Elasticsearch full	Index lifecycle management
Network congestion	Backpressure & retry queue
### 🧩 Optional Extensions

### 🧠 Content classifier (news/blog/docs detection).

### 🖼️ Image metadata extraction.

### 🤖 Semantic search using BERT embeddings.

### ⚙️ Domain-based rate limiting.

### 📈 Admin dashboard (stats, failures, throughput).

### 🧭 Skills & Concepts Learned
Area	Key Learning
Concurrency	Thread pools, async I/O, synchronization
Distributed Systems	Kafka coordination, partitioning, scaling
Search Systems	Inverted index, TF-IDF, ranking logic
Scalability	Stateless design, load balancing
Resilience	Fault isolation, message durability
DevOps	Docker, monitoring, observability
Architecture	Event-driven design & system decoupling
### 🧰 Recommended Tech Stack
Layer	Technologies
Language	Java 21+
Framework	Spring Boot 3+
Message Broker	Apache Kafka
Cache & Deduplication	Redis
Search Engine	Elasticsearch / Lucene
Database (metadata)	PostgreSQL
Containerization	Docker, Kubernetes
Monitoring	Prometheus, Grafana, ELK
Testing	JUnit, Testcontainers
## 🎨 Google-Like Web Interface

### Core Features

**Search Experience**
- Clean, minimalist design inspired by Google
- Instant search suggestions as you type
- Advanced search filters (date, domain, file type)
- "Did you mean?" spelling correction
- Search history and saved searches

**Results Display**
- Title, URL, and snippet for each result
- Pagination with infinite scroll option
- Related searches section
- Number of results and search time display
- Rich snippets with highlighted keywords

**UI/UX Components**
- Responsive design (mobile, tablet, desktop)
- Dark mode support
- Loading states and skeleton screens
- Smooth animations and transitions
- Keyboard shortcuts (/ to focus search)

**Tech Stack**
- **Framework**: Next.js 14 with App Router
- **Styling**: Tailwind CSS + shadcn/ui
- **State Management**: React Query for caching
- **Debouncing**: Optimized API calls for suggestions
- **SEO**: Server-side rendering for better performance

### Pages
1. **Home Page** (`/`) - Search box with Google-like minimal design
2. **Search Results** (`/search?q=...`) - Results list with filters
3. **About** (`/about`) - Project information
4. **Advanced Search** (`/advanced`) - Detailed search options

---

📅 Future Improvements

Domain-based crawl scheduling and fairness policy.

Multi-language tokenization and stemming.

Distributed ranking computation.

Image search capabilities.

API Gateway integration with authentication.

Personalized search results based on user preferences.

🧩 Summary

This project demonstrates the architecture of a search engine core built with event-driven microservices.
It emphasizes distributed crawling, asynchronous processing, full-text indexing, and horizontal scalability.

By completing it, you’ll gain hands-on understanding of:

Large-scale data ingestion pipelines,

Concurrency in distributed systems, and

How Google-style indexing and searching works under the hood.

## 🧰 Tech Stack

### 🏗️ Backend (Core Services)
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Language** | Java 21+ | Primary backend development language |
| **Framework** | Spring Boot 3+ | Microservice development, REST API, configuration management |
| **Concurrency** | Java ExecutorService / CompletableFuture | Multithreading for crawling and fetching pages |
| **Message Broker** | Apache Kafka | Event-driven communication between microservices (crawl, parse, index) |
| **Cache / Deduplication** | Redis | URL deduplication, caching, and rate-limiting |
| **Search Engine** | Elasticsearch / Apache Lucene | Full-text search and inverted indexing |
| **Database** | PostgreSQL | Metadata, crawl statistics, and configuration storage |
| **HTML Parser** | JSoup | HTML parsing and link extraction |
| **Scheduler** | Spring Scheduler / Quartz | Crawl scheduling and re-crawl interval management |
| **Containerization** | Docker | Packaging and running microservices |
| **Orchestration** | Kubernetes / Docker Compose | Scaling and service orchestration |
| **API Documentation** | Springdoc OpenAPI / Swagger | Interactive API docs |
| **Testing** | JUnit 5, Mockito, Testcontainers | Unit and integration testing for microservices |

---

### 🌐 Frontend (Search Interface)
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Language** | TypeScript | Type-safe frontend development |
| **Framework** | React 18 + Next.js 14 | Modern frontend with server-side rendering (SSR) |
| **UI Library** | Tailwind CSS / shadcn/ui | Styling, layout, and reusable components |
| **State Management** | React Query / Zustand | Query caching and global state handling |
| **HTTP Client** | Axios / Fetch API | Communication with backend Search API |
| **Routing** | Next.js App Router | Client-side and server-side routing |
| **Search Experience** | Debounced search, infinite scroll, instant suggestions | Smooth, responsive search interactions |
| **Authentication (Optional)** | NextAuth.js / JWT | Secure login for admin or restricted dashboards |

---

### 🔐 Security & Authentication
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Auth Framework** | Spring Security | Authentication, authorization, and API protection |
| **Token Format** | JWT (JSON Web Token) | Stateless user/session authentication |
| **CORS Management** | Spring Web MVC / Config | Secure cross-origin API access |
| **HTTPS Support** | Nginx reverse proxy / Spring Boot TLS | Encrypted communication |

---

### 📊 Observability & Monitoring
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Metrics Collection** | Micrometer + Prometheus | Application metrics (crawl rate, latency, errors) |
| **Visualization** | Grafana | Real-time dashboards for system health |
| **Logging** | Logback + ELK Stack (Elasticsearch, Logstash, Kibana) | Centralized logging and search |
| **Tracing** | OpenTelemetry / Zipkin | Distributed tracing between crawler, indexer, and API |
| **Health Checks** | Spring Boot Actuator | Liveness/readiness probes for Kubernetes |

---

### 🧩 DevOps & CI/CD
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Build Tool** | Maven / Gradle | Build automation and dependency management |
| **Version Control** | Git + GitHub  | Source control and project management |
| **CI/CD** | GitHub Actions  | Automated build, test, and deployment pipelines |
| **Container Registry** | Docker Hub / GitHub Packages | Image storage and versioning |
| **Infrastructure as Code** | Terraform / Helm (optional) | Automated environment provisioning |

---

### 🧠 Optional Enhancements
| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Semantic Search** | OpenAI Embeddings / HuggingFace | Context-aware content ranking |
| **Content Classification** | FastText / TensorFlow Lite | Categorize crawled pages (blog, news, docs) |
| **Rate Limiting** | Bucket4j / Redis | Prevent domain overload or IP blocking |
| **Analytics Dashboard** | Next.js + Chart.js / Recharts | Visualize crawl performance and search metrics |

---

🚀 Advanced Senior-Level Features:
14. Robots.txt Parser & Compliance ⚖️

RFC 9309 compliant robots.txt parsing
User-agent specific rules
Crawl-delay directive support
Wildcard pattern matching (* and $)
Sitemap.xml extraction
Intelligent caching (Redis + in-memory)
Async parsing to avoid blocking

15. Sitemap.xml Parser & Prioritization 🗺️

Handles regular sitemaps and sitemap indices
Supports gzipped sitemaps (.xml.gz)
Extracts URL priority, changefreq, lastmod
Nested sitemap index traversal
Intelligent priority calculation
Automatic dispatch to crawl queue
Respects lastmod for incremental crawling

16. Content-Based Deduplication (SimHash) 🔍

SimHash algorithm for near-duplicate detection
Detects content duplicates (not just URL duplicates)
Hamming distance calculation for similarity
Identifies mirror sites and scraped content
Batch duplicate detection in existing index
Automatic duplicate removal with PageRank preservation
Redis-based fingerprint caching

17. Distributed Locking & Advanced Rate Limiting 🔐

Redlock algorithm for distributed locks
Atomic lock acquisition with Lua scripts
Token bucket rate limiting (burst traffic support)
Sliding window rate limiting (prevents boundary gaming)
Per-domain fairness scheduling
Lock retry with exponential backoff
Comprehensive statistics and monitoring

18. Circuit Breaker & Resilience Patterns ⚡

Circuit breaker pattern (CLOSED/OPEN/HALF_OPEN states)
Automatic failure detection and recovery
Bulkhead pattern for resource isolation
Prevents cascading failures
Per-domain circuit breakers
Prometheus metrics integration
Configurable thresholds and timeouts

19. Query Expansion & Semantic Search 🧠

Synonym-based query expansion
Spelling correction ("did you mean?")
Entity detection (programming languages, years)
Intent classification (questions, tutorials)
Result diversification (max per domain)
Related searches generation
Advanced Elasticsearch queries with boosting
Function score for PageRank integration

20. Comprehensive Monitoring & Alerting 📊

Real-time metrics (throughput, latency, error rates)
Prometheus/Grafana integration
Queue depth monitoring
Circuit breaker health tracking
SLI/SLO tracking (efficiency, availability)
Automatic alert generation
Health checks for load balancers
Performance percentiles (p95, p99)

| Feature         | Senior-Level Aspects                                                                 |
|-----------------|-------------------------------------------------------------------------------------|
| Robots.txt      | RFC compliance, pattern matching, distributed caching                               |
| Sitemap         | Recursive parsing, gzip support, priority algorithms                                |
| SimHash         | Advanced hashing algorithm, Hamming distance, content similarity                     |
| Locking         | Distributed consensus, Lua atomic operations, fair scheduling                        |
| Circuit Breaker | State machines, failure prediction, auto-recovery                                    |
| Query Expansion | NLP techniques, synonym graphs, semantic understanding                               |
| Monitoring      | Production observability, SLO tracking, alerting pipelines                           |
