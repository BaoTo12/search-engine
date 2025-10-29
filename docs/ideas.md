🕸️ Distributed Crawler & Indexer (Search Engine Core)
🧭 Overview

This project aims to build a scalable, distributed web crawling and indexing system — the foundation of a search engine.
It will consist of multiple services (microservices or modules) that coordinate via Kafka and store structured data in Elasticsearch, enabling users to perform text-based searches through a REST API.

🎯 Goals

Crawl and index web content from multiple domains efficiently.

Scale horizontally (add more crawlers easily).

Store, tokenize, and rank indexed documents.

Provide search results through a REST or GraphQL API.

Maintain fault-tolerance and high throughput.

🧱 System Architecture Overview

+-------------------------------------------------------------+
|                    User Search Interface                    |
|               (REST / GraphQL Search API)                   |
+-----------------------------+-------------------------------+
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
Ranking Features:

TF-IDF weighting.

PageRank-style scoring (based on inbound links).

Optional: semantic relevance model.

5. 🔍 Search API Service

Purpose: Exposes endpoints to perform user queries against the indexed data.

Responsibilities:

Handle queries like /search?q=java+concurrency&page=1.

Translate to Elasticsearch syntax.

Rank, paginate, and format results.

Cache hot queries in Redis.

Log query metrics for analytics.

Example Response:

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

🧩 Data Flow Summary
🔹 Crawl Phase

Scheduler sends seed URLs → Kafka crawl-requests.

Worker fetches HTML → extracts links & content.

Extracted links → Kafka new-links.

Cleaned content → Kafka pages.

🔹 Index Phase

Indexer consumes from pages → tokenizes → stores in Elasticsearch.

Optional: offline ranking updates.

🔹 Search Phase

User sends query → Search API → queries Elasticsearch.

Results returned → ranked → paginated.

🚀 Scaling Strategy
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

🧠 Runtime Behavior

Crawl Burst: Kafka backpressure controls message flow.

Network Failures: Workers retry with exponential backoff; failed URLs → dead-letter queue.

Duplicates: Redis deduplication and checksum hashing.

Re-crawling: Scheduler revisits URLs periodically.

Ranking: Offline analyzer recalculates scores weekly.

📊 Monitoring & Observability

Prometheus + Grafana: Crawl rate, error rates, queue sizes.

Elastic APM: Trace end-to-end latency (crawler → indexer → search).

Kafka Lag Exporter: Track consumer lag.

ELK Stack: Log aggregation and search.

🔒 Fault Tolerance
Issue	Mitigation
Worker crash	Kafka rebalances consumer groups
Scheduler down	Redis retains pending URLs
Indexer failure	Kafka offsets prevent data loss
Elasticsearch full	Index lifecycle management
Network congestion	Backpressure & retry queue
🧩 Optional Extensions

🧠 Content classifier (news/blog/docs detection).

🖼️ Image metadata extraction.

🤖 Semantic search using BERT embeddings.

⚙️ Domain-based rate limiting.

📈 Admin dashboard (stats, failures, throughput).

🧭 Skills & Concepts Learned
Area	Key Learning
Concurrency	Thread pools, async I/O, synchronization
Distributed Systems	Kafka coordination, partitioning, scaling
Search Systems	Inverted index, TF-IDF, ranking logic
Scalability	Stateless design, load balancing
Resilience	Fault isolation, message durability
DevOps	Docker, monitoring, observability
Architecture	Event-driven design & system decoupling
🧰 Recommended Tech Stack
Layer	Technologies
Language	Java 17+
Framework	Spring Boot 3
Message Broker	Apache Kafka
Cache & Deduplication	Redis
Search Engine	Elasticsearch / Lucene
Database (metadata)	PostgreSQL
Containerization	Docker, Kubernetes
Monitoring	Prometheus, Grafana, ELK
Testing	JUnit, Testcontainers
📅 Future Improvements

Domain-based crawl scheduling and fairness policy.

Multi-language tokenization and stemming.

Distributed ranking computation.

Web-based search UI (React or Angular frontend).

API Gateway integration with authentication.

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
| **Language** | Java 17+ | Primary backend development language |
| **Framework** | Spring Boot 3 | Microservice development, REST API, configuration management |
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
| **Version Control** | Git + GitHub / GitLab | Source control and project management |
| **CI/CD** | GitHub Actions / Jenkins | Automated build, test, and deployment pipelines |
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
