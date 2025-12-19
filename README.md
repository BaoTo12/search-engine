# 🕸️ Enterprise Search Engine

A production-ready, distributed search engine with **Google-like web interface** and advanced crawling algorithms.

Built with **Spring Boot**, **Kafka**, **Elasticsearch**, **PostgreSQL**, **Redis**, and **Next.js**.

---

## ✨ Key Features

### 🧮 Advanced Crawling Algorithms
- **BFS (Breadth-First Search)** - Level-by-level exploration
- **Best-First Search** - Quality-focused prioritization  
- **OPIC (Online Page Importance)** - Real-time importance calculation
- **Focused Crawling** - Topic-specific targeting
- **Dynamic strategy switching** at runtime

### ⚡ Enterprise Services
- **Bloom Filter Deduplication** (10M URLs in ~12MB, 99% accuracy)
- **Token Bucket Rate Limiting** (distributed, per-domain)
- **PageRank Calculation** (iterative algorithm with weekly auto-update)
- **URL Normalization** (RFC 3986 compliant)
- **Query Expansion** (synonyms, spell-check, entity detection)

### 🖥️ Modern Web Interface
- **Google-like search experience** with autocomplete
- **Highlighted keywords** in search results
- **Responsive design** (mobile, tablet, desktop)
- **Dark mode support**
- **Real-time suggestions** with debouncing
- **Keyboard navigation** (Arrow keys, Enter, Escape)

### 🏗️ Distributed Architecture
- **Horizontal scaling** with multiple crawler workers
- **Event-driven** processing via Kafka
- **Full-text search** powered by Elasticsearch
- **Redis caching** for visited URLs and rate limiting
- **PostgreSQL** for metadata and PageRank storage

### 📊 Monitoring & Observability
- **Prometheus metrics** collection
- **Grafana dashboards**
- **Swagger API documentation**
- **Health checks** for all components
- **Statistics endpoints** for monitoring

---

## 🏗️ Architecture

```
                    ┌─────────────────────┐
                    │   Next.js Frontend  │
                    │  (Google-like UI)   │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │  Spring Boot API     │
                    │  (Search & Admin)   │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
┌───────▼────────┐   ┌────────▼────────┐   ┌────────▼────────┐
│ Elasticsearch  │   │     Kafka        │   │   PostgreSQL    │
│  (Full-Text    │   │  (Message Bus)   │   │   (Metadata)    │
│   Indexing)    │   └─────────┬────────┘   └─────────────────┘
└────────────────┘             │
                    ┌──────────▼──────────┐
                    │  Crawler Workers    │
                    │  (Parallel Fetch)   │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   URL Frontier      │
                    │  (Priority Queue)   │
                    └─────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Node.js 20+**
- **Docker & Docker Compose**
- **8GB+ RAM** (recommended)

### 1. Clone Repository
```bash
git clone https://github.com/BaoTo12/search-engine.git
cd search-engine
```

### 2. Start Infrastructure
```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka + Zookeeper (ports 9092, 2181)
- Elasticsearch (port 9200)
- Kibana (port 5601)
- Prometheus (port 9090)
- Grafana (port 3000 - conflicts with frontend, use 3001)
- Kafka UI (port 8090)

### 3. Build Backend
```bash
cd search-engine
mvn clean install -DskipTests
```

### 4. Run Backend
```bash
mvn spring-boot:run
```

Backend will start on http://localhost:8080

### 5. Run Frontend (Development)
```bash
cd ../search-engine-ui
npm install
npm run dev
```

Frontend will start on http://localhost:3000

### 6. Verify Services
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Kafka UI**: http://localhost:8090
- **Elasticsearch**: http://localhost:9200/_cluster/health

---

## 📖 Usage

### Add Seed URLs (Start Crawling)
```bash
curl -X POST http://localhost:8080/api/v1/admin/crawl/seeds \
  -H "Content-Type: application/json" \
  -d '["https://example.com", "https://www.iana.org"]'
```

### Search via API
```bash
curl "http://localhost:8080/api/v1/search?q=java+concurrency&page=0&size=10"
```

### Search via Web UI
1. Open http://localhost:3000
2. Enter search query
3. View results with highlighted keywords
4. Navigate with pagination

### Trigger PageRank Calculation
```bash
curl -X POST http://localhost:8080/api/v1/admin/indexer/pagerank/update
```

### Change Crawling Strategy
```bash
curl -X POST "http://localhost:8080/api/v1/admin/frontier/strategy?strategy=best-first"
```

Available strategies: `bfs`, `best-first`, `opic`, `focused`

### Get Statistics
```bash
# Crawler stats
curl http://localhost:8080/api/v1/admin/stats/crawler

# PageRank stats
curl http://localhost:8080/api/v1/admin/pagerank/stats

# URL Frontier stats
curl http://localhost:8080/api/v1/admin/frontier/stats

# Bloom Filter stats
curl http://localhost:8080/api/v1/admin/deduplication/stats
```

---

## 🧮 Advanced Algorithms Explained

### URL Prioritization Strategies

**BFS (Breadth-First Search)**
- Crawls all pages at depth N before moving to depth N+1
- Formula: `Priority = 1000 - depth`
- Best for: General-purpose crawling with good coverage

**Best-First Search**
- Prioritizes high-quality pages based on PageRank and domain authority
- Formula: `Priority = (PageRank × 0.7 + DomainAuthority × 0.3) / (depth + 1) × 1000`
- Best for: Limited crawl budgets, quality over quantity

**OPIC (Online Page Importance Computation)**
- Lightweight alternative to PageRank
- Formula: `Priority = cash × domainAuthority × freshness / log(depth + 2) × 1000`
- Best for: Large-scale crawling without expensive graph computation

**Focused Crawling**
- Targets specific topics/domains
- Uses domain whitelisting and URL pattern analysis
- Best for: Vertical search engines (academic, news, etc.)

### Bloom Filter Deduplication
- **Capacity**: 10 million URLs
- **False Positive Rate**: 1%
- **Memory Usage**: ~12MB
- **Time Complexity**: O(k) where k ≈ 7 hash functions
- **Redis Fallback**: 100% accurate verification on potential duplicates

### Token Bucket Rate Limiting
- **Algorithm**: Token bucket with burst support
- **Configuration**: 10 requests/bucket, 1 token/second refill
- **Implementation**: Redis Lua scripts for atomic operations
- **Features**: Per-domain limits, wait time calculation

### PageRank Calculation
- **Algorithm**: Iterative PageRank with damping factor 0.85
- **Convergence**: Typically 10-15 iterations
- **Formula**: `PR(A) = (1-d) + d × Σ(PR(Ti) / C(Ti))`
- **Schedule**: Weekly automatic recalculation (Sundays 2 AM)

---

## 🔧 Configuration

### Backend (application.yml)
```yaml
crawler:
  batch-size: 100
  max-depth: 3
  threads:
    core-pool-size: 20
    max-pool-size: 100

indexer:
  batch-size: 50
  max-tokens: 10000

search:
  cache:
    ttl-minutes: 30
```

### Frontend (.env.local)
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## 📊 Database Schema

### Tables

**crawl_urls** - URL crawl queue
- `id`, `url`, `url_hash`, `domain`, `status`, `depth`, `priority`, `last_crawl_attempt`, `failure_count`

**domain_metadata** - Per-domain configuration
- `id`, `domain`, `crawl_delay_ms`, `max_concurrent_requests`, `is_blocked`, `total_pages_crawled`

**web_pages** - Indexed content (Elasticsearch)
- `url`, `title`, `content`, `domain`, `indexed_at`

**page_links** - Link graph for PageRank
- `id`, `source_url`, `target_url`, `anchor_text`, `created_at`

**page_ranks** - Calculated PageRank scores
- `id`, `url`, `rank_score`, `inbound_links`, `outbound_links`, `last_calculated`

---

## 🧪 Testing

### Run Backend Tests
```bash
cd search-engine
mvn test
```

### Run with Coverage
```bash
mvn test jacoco:report
```

### Manual End-to-End Test
1. Start all services
2. Add seed URLs
3. Wait for crawling (check Kafka UI)
4. Verify indexing (check Elasticsearch)
5. Search via web UI
6. Check PageRank stats

---

## 🐳 Docker Deployment

### Build and Run All Services
```bash
docker-compose up --build
```

### Run in Detached Mode
```bash
docker-compose up -d
```

### View Logs
```bash
docker-compose logs -f
```

### Stop All Services
```bash
docker-compose down
```

---

## 📈 Scaling

### Horizontal Scaling

**Crawler Workers**
```yaml
spring:
  kafka:
    listener:
      concurrency: 10  # 10 consumer threads
```

**Multiple Application Instances**
```bash
java -jar search-engine.jar --server.port=8081 &
java -jar search-engine.jar --server.port=8082 &
```

**Kafka Partitions**
```bash
kafka-topics --alter --topic crawl-requests \
  --partitions 10 --bootstrap-server localhost:9092
```

### Vertical Scaling
```bash
export JAVA_OPTS="-Xms4g -Xmx8g"
mvn spring-boot:run
```

---

## 🔒 Security Considerations

- [ ] Add authentication to admin endpoints (Spring Security + JWT)
- [ ] Enable HTTPS in production
- [ ] Implement rate limiting on search API
- [ ] Secure Kafka with SASL/SSL
- [ ] Enable Elasticsearch security features
- [ ] Use secrets management (Vault, AWS Secrets Manager)
- [ ] Add CORS configuration for production domains

---

## 🐛 Troubleshooting

### Kafka Connection Issues
```bash
docker logs crawler-kafka
docker exec -it crawler-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Elasticsearch Connection Issues
```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/_cat/indices?v
```

### Frontend Cannot Connect to Backend
- Check if backend is running on port 8080
- Verify CORS configuration in Spring Boot
- Check Next.js proxy rewrite rules in `next.config.ts`

---

## 📚 API Documentation

Full API documentation available at: **http://localhost:8080/swagger-ui.html**

### Key Endpoints

**Search**
- `GET /api/v1/search?q={query}&page={page}&size={size}` - Search
- `GET /api/v1/search/suggestions?prefix={prefix}` - Autocomplete

**Admin**
- `POST /api/v1/admin/crawl/seeds` - Add seed URLs
- `GET /api/v1/admin/stats/crawler` - Crawler statistics
- `POST /api/v1/admin/indexer/pagerank/update` - Trigger PageRank
- `GET /api/v1/admin/pagerank/stats` - PageRank statistics
- `POST /api/v1/admin/frontier/strategy?strategy={name}` - Change strategy
- `GET /api/v1/admin/rate-limit/{domain}` - Check rate limit
- `POST /api/v1/admin/rate-limit/{domain}/reset` - Reset rate limit

---

## 🎯 Performance Metrics

### Targets
- ✅ Search response time: < 500ms (p95)
- ✅ Crawler throughput: 100+ pages/second
- ✅ Bloom filter: 10M URLs in 12MB
- ✅ PageRank convergence: < 20 iterations
- ✅ Frontend Lighthouse score: > 90

### Monitoring
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (or 3001)
- Kafka UI: http://localhost:8090

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Apache License 2.0

---

## 👤 Contact

**BaoTo** - baotochi87@gmail.com

Project Link: [https://github.com/BaoTo12/search-engine](https://github.com/BaoTo12/search-engine)

---

## 🎓 Learning Outcomes

This project demonstrates:
- ✅ Advanced crawling algorithms (BFS, Best-First, OPIC, Focused)
- ✅ Distributed systems with Kafka
- ✅ Full-text search with Elasticsearch
- ✅ PageRank implementation
- ✅ Modern full-stack development (Spring Boot + Next.js)
- ✅ Docker containerization
- ✅ Microservices architecture
- ✅ Event-driven design
- ✅ Production-ready observability

---

**⭐ Built with enterprise-grade patterns for production use!**


A scalable, distributed web crawling and indexing system built with **Spring Boot**, **Kafka**, **Elasticsearch**, and **PostgreSQL**.

## Key Features

- **Distributed Crawling**: Horizontal scaling with multiple crawler workers
- **Event-Driven Architecture**: Asynchronous processing via Kafka
- **Full-Text Search**: Powered by Elasticsearch with TF-IDF ranking
- **URL Deduplication**: Redis-based visited URL tracking
- **Rate Limiting**: Politeness policy for domain-specific crawl delays
- **PageRank Scoring**: Link-based relevance ranking
- **REST API**: Search and admin endpoints with Swagger documentation
- **Monitoring**: Prometheus metrics and Grafana dashboards
- **Fault Tolerance**: Retry logic, dead letter queues, and graceful error handling

## Key Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User Search Interface                    │
│                  (REST API / GraphQL API)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           â"‚
                  ┌────────▼────────┐
                  │  Search Service  │──────► Elasticsearch
                  └────────▲────────┘
                           │
                  ┌────────┴────────┐
                  │ Indexing Service │
                  └────────▲────────┘
                           │
           ┌───────────────┴───────────────┐
           │   Kafka Topics (Messages)     │
           │  • crawl-requests             │
           │  • index-requests             │
           │  • link-discoveries           │
           └───────────────▲───────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │      Crawler Workers (Parallel)      │
        │  Fetch → Parse → Extract → Publish   │
        └──────────────────▲──────────────────┘
                           │
                  ┌────────┴────────┐
                  │ Crawl Scheduler  │
                  │ (URL Frontier)   │
                  └────────▲────────┘
                           │
                  ┌────────┴────────┐
                  │  PostgreSQL DB   │
                  │  (Metadata)      │
                  └─────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.2 |
| **Message Broker** | Apache Kafka |
| **Search Engine** | Elasticsearch 8.11 |
| **Database** | PostgreSQL 15 |
| **Cache** | Redis 7 |
| **Monitoring** | Prometheus + Grafana |
| **Containerization** | Docker + Docker Compose |
| **API Docs** | Springdoc OpenAPI (Swagger) |

## Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Docker & Docker Compose** (for infrastructure)
- **8GB+ RAM** (recommended for running all services)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/distributed-crawler.git
cd distributed-crawler
```

### 2. Start Infrastructure Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka + Zookeeper (ports 9092, 2181)
- Elasticsearch (port 9200)
- Kibana (port 5601)
- Prometheus (port 9090)
- Grafana (port 3000)
- Kafka UI (port 8090)

### 3. Build the Application

```bash
mvn clean install -DskipTests
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or with a specific profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Verify Services

- **Application**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Kafka UI**: http://localhost:8090
- **Elasticsearch**: http://localhost:9200/_cluster/health
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

## Monitoring Dashboards

### Grafana Dashboard

1. Open http://localhost:3000
2. Login with `admin/admin`
3. Add Prometheus data source: `http://prometheus:9090`
4. Import dashboard JSON (provided in `/monitoring` folder)

### Prometheus Metrics

Available at: http://localhost:9090/targets

Key metrics:
- `crawler.success` - Successful crawls count
- `crawler.failure` - Failed crawls count
- `indexer.indexed` - Indexed documents count

## API Usage Examples

### Add Seed URLs

```bash
curl -X POST http://localhost:8080/api/v1/admin/crawl/seeds \
  -H "Content-Type: application/json" \
  -d '["https://example.com", "https://wikipedia.org"]'
```

### Search for Content

```bash
curl "http://localhost:8080/api/v1/search?q=java+concurrency&page=0&size=10"
```

### Get Search Suggestions

```bash
curl "http://localhost:8080/api/v1/search/suggestions?prefix=ja"
```

### Get Crawler Statistics

```bash
curl http://localhost:8080/api/v1/admin/stats/crawler
```

### Trigger PageRank Update

```bash
curl -X POST http://localhost:8080/api/v1/admin/indexer/pagerank/update
```

## ðŸ§ª Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CrawlerWorkerServiceTest

# Run with coverage
mvn test jacoco:report
```

## Configuration

### Application Properties

Edit `src/main/resources/application.yml`:

```yaml
crawler:
  batch-size: 100
  max-depth: 3
  threads:
    core-pool-size: 20
    max-pool-size: 100

indexer:
  batch-size: 50
  max-tokens: 10000

search:
  cache:
    ttl-minutes: 30
```

### Environment Variables

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/crawler_db
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_ELASTICSEARCH_URIS=http://localhost:9200
export SPRING_DATA_REDIS_HOST=localhost
```

## Kafka Topics

The system uses the following Kafka topics:

| Topic | Purpose | Partitions |
|-------|---------|------------|
| `crawl-requests` | URLs to crawl | 3 |
| `index-requests` | Pages to index | 2 |
| `link-discoveries` | Newly discovered links | 2 |
| `crawl-dlq` | Failed crawl attempts | 1 |

### Create Topics Manually (if needed)

```bash
# Connect to Kafka container
docker exec -it crawler-kafka bash

# Create topic
kafka-topics --create --topic crawl-requests \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

## Database Schema

### Tables

**crawl_urls**
- `id` (Primary Key)
- `url` (Unique)
- `url_hash` (SHA-256, Indexed)
- `domain` (Indexed)
- `status` (Enum: PENDING, IN_PROGRESS, COMPLETED, FAILED)
- `depth`
- `priority`
- `last_crawl_attempt`
- `failure_count`

**domain_metadata**
- `id` (Primary Key)
- `domain` (Unique)
- `crawl_delay_ms`
- `max_concurrent_requests`
- `is_blocked`
- `total_pages_crawled`
- `total_failures`

## Scaling

### Horizontal Scaling

1. **Crawler Workers**: Increase Kafka consumer concurrency
```yaml
spring:
  kafka:
    listener:
      concurrency: 5  # Number of consumer threads
```

2. **Multiple Application Instances**: Run multiple JARs with same config
```bash
java -jar crawler.jar --server.port=8081 &
java -jar crawler.jar --server.port=8082 &
```

3. **Kafka Partitions**: Increase topic partitions for better parallelism
```bash
kafka-topics --alter --topic crawl-requests \
  --partitions 6 --bootstrap-server localhost:9092
```

### Vertical Scaling

Increase JVM heap size:
```bash
export JAVA_OPTS="-Xms2g -Xmx4g"
mvn spring-boot:run
```

## Security Considerations

- Add authentication to admin endpoints
- Use HTTPS in production
- Implement rate limiting on search API
- Secure Kafka with SASL/SSL
- Enable Elasticsearch security features
- Use secrets management (Vault, AWS Secrets Manager)

## Troubleshooting

### Kafka Connection Issues

```bash
# Check Kafka logs
docker logs crawler-kafka

# Verify Kafka is running
docker exec -it crawler-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### Elasticsearch Connection Issues

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# View indices
curl http://localhost:9200/_cat/indices?v
```

### High Memory Usage

- Reduce batch sizes in configuration
- Decrease crawler thread pool size
- Limit Elasticsearch JVM heap size


Apache License 2.0

##Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Contact

chibao - baotochi87@gmail.com

