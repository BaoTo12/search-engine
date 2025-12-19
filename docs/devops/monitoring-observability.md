# Monitoring & Observability

> **Prometheus metrics + Grafana dashboards + CloudWatch alarms**

---

## Architecture

```
Application (Spring Boot)
    ↓ /actuator/prometheus
Prometheus (scrape metrics)
    ↓
Grafana (visualize)
    ↓
AlertManager (notify)
    ↓
Slack/Email/PagerDuty
```

---

## Spring Boot Monitoring

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
  
  health:
    db:
      enabled: true
    redis:
      enabled: true
    kafka:
      enabled: true
```

### Custom Metrics

```java
@Service
public class MonitoringService {
    
    private final MeterRegistry meterRegistry;
    private final Counter crawlRequestCounter;
    private final Timer crawlDurationTimer;
    private final Gauge urlQueueSize;
    
    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Counter: Total crawl requests
        this.crawlRequestCounter = Counter.builder("crawl_requests_total")
            .description("Total number of crawl requests")
            .tag("status", "success")
            .register(meterRegistry);
        
        // Timer: Crawl duration
        this.crawlDurationTimer = Timer.builder("crawl_duration_seconds")
            .description("Time taken to crawl a page")
            .register(meterRegistry);
        
        // Gauge: URL queue size
        this.urlQueueSize = Gauge.builder("url_queue_size", this::getQueueSize)
            .description("Number of URLs in crawl queue")
            .register(meterRegistry);
    }
    
    public void recordCrawlRequest(String domain, boolean success) {
        crawlRequestCounter.increment();
        
        Counter.builder("crawl_requests_by_domain")
            .tag("domain", domain)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCrawlDuration(long durationMs) {
        crawlDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    private double getQueueSize() {
        return crawlUrlRepository.countByStatus(CrawlStatus.PENDING);
    }
}
```

---

## Prometheus Configuration

**prometheus.yml**

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'search-engine-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          environment: 'production'
          service: 'backend'
  
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - '/etc/prometheus/alerts.yml'
```

**alerts.yml**

```yaml
groups:
  - name: search_engine_alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(crawl_requests_total{success="false"}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High crawl error rate"
          description: "Error rate is {{ $value }} errors/sec"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High JVM memory usage"
          description: "Memory usage: {{ $value | humanizePercentage }}"
      
      - alert: ServiceDown
        expr: up{job="search-engine-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Backend service is down"
```

---

## Grafana Dashboard

**dashboard.json** (excerpt)

```json
{
  "dashboard": {
    "title": "Search Engine Monitoring",
    "panels": [
      {
        "id": 1,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      {
        "id": 2,
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "Used Heap"
          },
          {
            "expr": "jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "Max Heap"
          }
        ]
      },
      {
        "id": 3,
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          }
        ]
      }
    ]
  }
}
```

---

## Key Metrics to Monitor

### Application Metrics

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Request duration (p95, p99)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Active threads
jvm_threads_live_threads

# GC pressure
rate(jvm_gc_pause_seconds_count[5m])
```

### Business Metrics

```promql
# Crawl throughput
rate(crawl_requests_total[5m])

# URL queue size
url_queue_size

# PageRank calculation time
pagerank_calculation_duration_seconds

# Index size
elasticsearch_doc_count
```

### Infrastructure Metrics

```promql
# CPU usage
rate(process_cpu_seconds_total[5m])

# Database connections
hikaricp_connections{pool="HikariPool-1"}

# Kafka lag
kafka_consumergroup_lag

# Redis memory
redis_memory_used_bytes
```

---

## CloudWatch Integration (AWS)

```java
@Configuration
public class CloudWatchConfig {
    
    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
    
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(
            CloudWatchConfig config,
            CloudWatchAsyncClient client) {
        
        return new CloudWatchMeterRegistry(config, Clock.SYSTEM, client);
    }
}
```

### CloudWatch Alarms via Terraform

```hcl
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "search-engine-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  
  dimensions = {
    ServiceName = "search-engine-backend-service"
    ClusterName = "search-engine-prod-cluster"
  }
  
  alarm_actions = [aws_sns_topic.alerts.arn]
}
```

---

*Comprehensive monitoring ensures system reliability and fast incident response!*
