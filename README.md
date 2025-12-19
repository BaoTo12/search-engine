# 🔍 Enterprise-Grade Distributed Search Engine

> A production-ready search engine demonstrating advanced algorithms, distributed systems patterns, and enterprise architecture

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-3.x-black.svg)](https://kafka.apache.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-yellow.svg)](https://www.elastic.co/)

---

## 🌟 What Makes This Project Special

This is **NOT** a toy search engine. It implements production-grade algorithms and distributed systems patterns used by companies like Google, demonstrating **senior-level engineering**.

### 🎯 10 Unique Features

| # | Feature | Why It's Special | Tech Highlight |
|---|---------|------------------|----------------|
| 1️⃣ | **PageRank Algorithm** | Full graph-based ranking with convergence detection | Power iteration, damping factor 0.85 |
| 2️⃣ | **4 Crawling Strategies** | Pluggable algorithms (BFS, Best-First, OPIC, Focused) | Strategy pattern, Redis-based priority queue |
| 3️⃣ | **SimHash Deduplication** | Near-duplicate detection in O(1) | 64-bit fingerprints, Hamming distance |
| 4️⃣ | **Distributed Crawling** | Domain-partitioned Kafka consumers | Consistent hashing, automatic load balancing |
| 5️⃣ | **Bloom Filters** | 10M URLs in 12MB memory (1% FPR) | Probabilistic data structure, two-layer verification |
| 6️⃣ | **Query Expansion** | Spell check, synonyms, entity detection | Levenshtein distance, NLP techniques |
| 7️⃣ | **Token Bucket Rate Limiter** | Per-domain politeness with Redis | Atomic Lua scripts, distributed rate limiting |
| 8️⃣ | **Circuit Breaker** | Fault tolerance with auto-recovery | State machine (CLOSED/OPEN/HALF_OPEN) |
| 9️⃣ | **Distributed Locking** | Redlock algorithm for coordination | Redis SET NX, atomic operations |
| 🔟 | **Robots.txt Compliance** | RFC 9309 compliant parser | Wildcard matching, sitemap extraction |

**See detailed docs:** [`docs/features/`](docs/features/)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│           Next.js Frontend (Google-like UI)             │
└─────────────────────────────────────────────────────────┘
                         ↓ HTTP
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Search API                     │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐│
│  │ Query        │→ │ Elasticsearch│→ │ PageRank      ││
│  │ Expansion    │  │ Full-Text    │  │ Boost         ││
│  └──────────────┘  └──────────────┘  └───────────────┘│
└─────────────────────────────────────────────────────────┘
                         ↑
┌─────────────────────────────────────────────────────────┐
│                 Kafka Message Broker                    │
│     crawl-requests │ pages │ new-links                  │
└─────────────────────────────────────────────────────────┘
         ↑                    ↓                  ↑
┌────────────────┐   ┌────────────────┐   ┌─────────────┐
│ Crawl          │   │ Indexer        │   │ Link        │
│ Scheduler      │   │ Service        │   │ Extractor   │
│ + URL Frontier │   │ (Elasticsearch)│   │             │
└────────────────┘   └────────────────┘   └─────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│            Crawler Workers (Parallel)                   │
│  robots.txt → HTTP GET → HTML Parse → Extract Links    │
└─────────────────────────────────────────────────────────┘
```

**Event-Driven Microservices** with Kafka for async, scalable processing

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.8+
- Node.js 18+ (for frontend)

### Run Entire System

```bash
# 1. Start infrastructure (Postgres, Redis, Kafka, Elasticsearch)
docker-compose up -d

# 2. Build and run backend
cd search-engine
mvn clean package -DskipTests
java -jar target/search-engine-0.0.1-SNAPSHOT.jar

# 3. Add seed URLs
curl -X POST http://localhost:8080/api/v1/admin/crawl/seeds \
  -H "Content-Type: application/json" \
  -d '["https://example.com", "https://wikipedia.org"]'

# 4. Start frontend (optional)
cd ../search-engine-ui
npm install
npm run dev
```

**Access:**
- Backend API: http://localhost:8080
- Frontend: http://localhost:3000
- API Docs: http://localhost:8080/swagger-ui.html
- Prometheus Metrics: http://localhost:8080/actuator/prometheus

---

## 📊 Key Algorithms Explained

### 1. PageRank Algorithm
**Formula:** `PR(A) = (1-d) + d × Σ(PR(Ti) / C(Ti))`

Calculates page importance based on link structure. Uses power iteration with convergence detection.

**[Read detailed docs →](docs/features/pagerank-algorithm.md)**

### 2. SimHash Content Deduplication
**Hash:** 64-bit fingerprint using weighted features

Detects near-duplicate content in O(1) using Hamming distance ≤ 3.

**[Read detailed docs →](docs/features/content-deduplication-simhash.md)**

### 3. OPIC (Online Page Importance)
**Model:** Cash distribution - pages transfer "cash" to outbound links

Real-time priority calculation without global graph analysis.

**[Read detailed docs →](docs/features/url-prioritization-strategies.md)**

### 4. Bloom Filters
**Math:** `FPR = (1 - e^(-kn/m))^k`

10 million URLs in 12MB with 1% false positive rate.

**[Read detailed docs →](docs/features/bloom-filters.md)**

---

## 🎯 Project Structure

```
search-engine/
├── docs/                          # Documentation
│   ├── features/                  # Individual feature deep-dives
│   ├── ideas.md                   # Original architecture design
│   └── advanced-deep-dive.md      # Comprehensive guide
├── search-engine/                 # Backend (Spring Boot)
│   ├── src/main/java/
│   │   └── com/chibao/edu/search_engine/
│   │       ├── service/           # Business logic ⭐
│   │       ├── strategy/          # URL prioritization algorithms
│   │       ├── components/        # Circuit breakers, parsers
│   │       ├── repository/        # Data access
│   │       └── controller/        # REST API
│   └── src/main/resources/
│       └── db/migration/          # Flyway SQL migrations
├── search-engine-ui/              # Frontend (Next.js)
└── docker-compose.yml             # Infrastructure
```

---

## 🛠️ Technology Stack

### Backend
- **Java 21** - Modern Java with virtual threads
- **Spring Boot 3.x** - Microservices framework
- **Apache Kafka** - Event streaming (crawl-requests, pages, new-links)
- **Elasticsearch** - Full-text search engine
- **PostgreSQL** - Metadata storage (crawl queue, PageRank)
- **Redis** - Caching, distributed locks, rate limiting
- **Flyway** - Database migrations

### Frontend
- **Next.js 14** - React framework with SSR
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **React Query** - Server state management

### Observability
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics storage
- **Spring Boot Actuator** - Health checks

---

## 📚 Documentation

### For Beginners
- [Quick Start Guide](docs/project-deep-dive-guide.md) - Get running in 5 minutes
- [Architecture Overview](docs/ideas.md) - System design and components

### For Advanced Developers
- [Advanced Deep Dive](docs/advanced-deep-dive.md) - Algorithms & distributed systems
- [Feature Documentation](docs/features/) - Individual feature deep-dives

### API Reference
- Swagger UI: http://localhost:8080/swagger-ui.html
- Postman Collection: `docs/postman-collection.json` (TODO)

---

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn verify -P integration-tests

# Load testing
cd load-tests
./run-load-test.sh
```

---

## 📈 Performance

**Benchmarks** (single node, 8 cores, 16GB RAM):
- Crawling: **200 pages/min** (with politeness delays)
- Indexing: **1000 docs/sec** to Elasticsearch
- Search: **p99 latency < 100ms** for 10M documents
- PageRank: **1M pages in ~30 seconds**

---

## 🏆 What You'll Learn

By studying this project, you'll understand:

✅ How Google Search works (simplified but architecturally sound)  
✅ PageRank algorithm with graph theory  
✅ Distributed systems with Kafka  
✅ Probabilistic data structures (Bloom filters, SimHash)  
✅ NLP techniques (spell check, query expansion)  
✅ Resilience patterns (circuit breakers, rate limiting)  
✅ Event-driven architecture  
✅ Production-ready Spring Boot  
✅ Full-text search with Elasticsearch  
✅ Distributed locking and coordination  

---

## 🤝 Contributing

This is an educational/portfolio project. If you want to extend it:

1. Fork the repository
2. Create a feature branch
3. Implement with tests
4. Submit a pull request

---

## 📝 License

MIT License - See [LICENSE](LICENSE) file

---

## 👨‍💻 Author

**Bao To**  
Senior Software Engineer specializing in distributed systems and search technologies

---

## ⭐ Star This Project!

If you found this helpful for learning search engines or distributed systems, please star the repo!

**Questions?** Open an issue or check the [documentation](docs/).
