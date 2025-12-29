# Deployment Guide - Search Engine

## Production Deployment Checklist

### Pre-Deployment

- [ ] Environment variables configured
- [ ] Database migrations tested
- [ ] Kafka topics created
- [ ] Elasticsearch indices configured
- [ ] SSL/TLS certificates installed
- [ ] Monitoring dashboards configured
- [ ] Load testing completed
- [ ] Security audit passed

### Infrastructure Setup

#### 1. Database Setup
```bash
# Create database and user
psql -U postgres
CREATE DATABASE search_engine;
CREATE USER search_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE search_engine TO search_user;
```

#### 2. Kafka Topics
```bash
# Create topics with proper replication
kafka-topics --create --topic crawl-requests --partitions 10 --replication-factor 3 --bootstrap-server localhost:9092
kafka-topics --create --topic pages --partitions 10 --replication-factor 3 --bootstrap-server localhost:9092
kafka-topics --create --topic new-links --partitions 10 --replication-factor 3 --bootstrap-server localhost:9092
kafka-topics --create --topic dead-letter-queue --partitions 3 --replication-factor 3 --bootstrap-server localhost:9092
```

#### 3. Elasticsearch Indices
```bash
# Create index with custom mapping
curl -X PUT "localhost:9200/web_pages" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 5,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "english_analyzer": {
          "type": "standard",
          "stopwords": "_english_"
        }
      }
    }
  }
}'
```

### Application Deployment

#### Docker Deployment
```bash
# Build Docker image
docker build -t search-engine:latest .

# Run with Docker Compose
docker-compose -f docker-compose.prod.yml up -d

# Check logs
docker-compose logs -f search-engine
```

#### Kubernetes Deployment
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml

# Check deployment
kubectl get pods -n search-engine
kubectl logs -f deployment/search-engine -n search-engine
```

### Environment Variables

```bash
# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/search_engine
SPRING_DATASOURCE_USERNAME=search_user
SPRING_DATASOURCE_PASSWORD=your_secure_password

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092

# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your_redis_password

# Elasticsearch
SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200

# Monitoring
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

### Post-Deployment

#### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

#### 2. Add Initial Seed URLs
```bash
curl -X POST http://localhost:8080/api/v1/admin/crawl/seeds \
  -H "Content-Type: application/json" \
  -d '{
    "urls": ["https://example.com"],
    "maxDepth": 3,
    "priority": 1.0
  }'
```

#### 3. Monitor Metrics
```bash
# Check Prometheus targets
open http://localhost:9090/targets

# Check Grafana dashboards
open http://localhost:3000
```

### Scaling

#### Horizontal Scaling
```bash
# Scale crawler workers
kubectl scale deployment search-engine-crawler --replicas=5

# Scale search API
kubectl scale deployment search-engine-api --replicas=3
```

#### Kafka Partitioning
```bash
# Increase partitions for better parallelism
kafka-topics --alter --topic pages --partitions 20 --bootstrap-server localhost:9092
```

### Backup & Recovery

#### Database Backup
```bash
# Automated daily backup
pg_dump -U search_user search_engine | gzip > backup_$(date +%Y%m%d).sql.gz

# Restore
gunzip < backup_20231229.sql.gz | psql -U search_user search_engine
```

#### Elasticsearch Snapshot
```bash
# Create snapshot repository
curl -X PUT "localhost:9200/_snapshot/backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/elasticsearch"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/backup/snapshot_1?wait_for_completion=true"
```

### Troubleshooting

#### High CPU Usage
1. Check Kafka consumer lag: `kafka-consumer-groups --describe --group crawler-workers`
2. Review slow queries in PostgreSQL
3. Check Elasticsearch query performance

#### Memory Issues
1. Tune JVM heap: `-Xms2g -Xmx4g`
2. Optimize Elasticsearch heap
3. Review Redis memory usage

#### Network Issues
1. Check Kafka connectivity
2. Verify Elasticsearch cluster health
3. Test PostgreSQL connection pooling

### Security

#### SSL/TLS Configuration
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

#### Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: search-engine-policy
spec:
  podSelector:
    matchLabels:
      app: search-engine
  ingress:
    - from:
      - podSelector:
          matchLabels:
            role: frontend
```

### Performance Tuning

#### Application
```properties
# Thread pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10

# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Kafka
spring.kafka.consumer.max-poll-records=100
spring.kafka.consumer.fetch-min-size=1024
```

#### PostgreSQL
```sql
-- Increase shared buffers
ALTER SYSTEM SET shared_buffers = '4GB';

-- Increase work mem
ALTER SYSTEM SET work_mem = '64MB';

-- Reload config
SELECT pg_reload_conf();
```

### Monitoring Alerts

#### Prometheus Alerts
```yaml
groups:
  - name: search-engine
    rules:
      - alert: HighErrorRate
        expr: rate(crawler_requests_failure_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High crawl error rate"
          
      - alert: CircuitBreakerOpen
        expr: circuit_breaker_state{state="OPEN"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker is OPEN"
```

## Support

For deployment issues, contact: support@example.com
