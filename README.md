#  Distributed Crawler & Indexer (Search Engine Core)

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

