package com.chibao.edu.search_engine.monitoring;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.components.CircuitBreakerRegistry;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.elasticsearch.WebPageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Senior Level: Comprehensive Monitoring, Metrics & Health Checks
 * <p>
 * Monitors:
 * - Crawl throughput and latency
 * - Index growth rate
 * - Error rates and types
 * - Queue depths (Kafka lag)
 * - Resource utilization
 * - Circuit breaker states
 * - Domain health scores
 * <p>
 * Provides:
 * - Real-time dashboards (Prometheus/Grafana)
 * - Health endpoints for load balancers
 * - Alerting triggers
 * - Performance SLIs/SLOs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final CrawlUrlRepository crawlUrlRepository;
    private final WebPageRepository webPageRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final KafkaListenerEndpointRegistry kafkaListenerRegistry;

    // Throughput tracking
    private final AtomicLong crawlsLastMinute = new AtomicLong(0);
    private final AtomicLong indexesLastMinute = new AtomicLong(0);

    // Latency tracking
    private Timer crawlLatencyTimer;
    private Timer indexLatencyTimer;
    private Timer searchLatencyTimer;

    // Gauges for queue depths
    private Gauge pendingCrawlsGauge;
    private Gauge indexSizeGauge;

    // Health status
    private volatile HealthStatus systemHealth = HealthStatus.HEALTHY;

    @PostConstruct
    public void init() {
        registerMetrics();
    }

    /**
     * Register custom metrics
     */
    private void registerMetrics() {
        // Latency timers
        crawlLatencyTimer = Timer.builder("crawler.latency")
                .description("Time taken to crawl a page")
                .tag("component", "crawler")
                .register(meterRegistry);

        indexLatencyTimer = Timer.builder("indexer.latency")
                .description("Time taken to index a page")
                .tag("component", "indexer")
                .register(meterRegistry);

        searchLatencyTimer = Timer.builder("search.latency")
                .description("Time taken for search query")
                .tag("component", "search")
                .register(meterRegistry);

        // Queue depth gauges
        pendingCrawlsGauge = Gauge.builder("crawler.queue.pending",
                () -> crawlUrlRepository.countByStatus(CrawlStatus.PENDING))
                .description("Number of URLs pending crawl")
                .register(meterRegistry);

        indexSizeGauge = Gauge.builder("index.size",
                () -> webPageRepository.count())
                .description("Total number of indexed documents")
                .register(meterRegistry);

        // Throughput gauges
        Gauge.builder("crawler.throughput.per_minute", crawlsLastMinute::get)
                .description("Crawls completed in last minute")
                .register(meterRegistry);

        Gauge.builder("indexer.throughput.per_minute", indexesLastMinute::get)
                .description("Documents indexed in last minute")
                .register(meterRegistry);

        // System health gauge
        Gauge.builder("system.health",
                () -> systemHealth.ordinal())
                .description("Overall system health (0=healthy, 1=degraded, 2=unhealthy)")
                .register(meterRegistry);
    }

    /**
     * Record crawl operation
     */
    public void recordCrawl(long durationMs, boolean success) {
        crawlLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
        crawlsLastMinute.incrementAndGet();

        if (!success) {
            Counter.builder("crawler.errors")
                    .description("Number of crawl errors")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Record index operation
     */
    public void recordIndex(long durationMs, boolean success) {
        indexLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
        indexesLastMinute.incrementAndGet();

        if (!success) {
            Counter.builder("indexer.errors")
                    .description("Number of indexing errors")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Record search operation
     */
    public void recordSearch(long durationMs, int resultCount) {
        searchLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("search.queries")
                .description("Total search queries")
                .register(meterRegistry)
                .increment();

        if (resultCount == 0) {
            Counter.builder("search.zero_results")
                    .description("Searches with no results")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Comprehensive health check
     */
    public HealthReport getHealthReport() {
        HealthReport report = new HealthReport();
        report.setTimestamp(LocalDateTime.now());

        // Database health
        try {
            long urlCount = crawlUrlRepository.count();
            report.addComponent("database", HealthStatus.HEALTHY,
                    "URL count: " + urlCount);
        } catch (Exception e) {
            report.addComponent("database", HealthStatus.UNHEALTHY,
                    "Error: " + e.getMessage());
        }

        // Elasticsearch health
        try {
            long indexSize = webPageRepository.count();
            report.addComponent("elasticsearch", HealthStatus.HEALTHY,
                    "Index size: " + indexSize);
        } catch (Exception e) {
            report.addComponent("elasticsearch", HealthStatus.UNHEALTHY,
                    "Error: " + e.getMessage());
        }

        // Kafka health
        try {
            boolean kafkaHealthy = kafkaListenerRegistry.getAllListenerContainers()
                    .stream()
                    .allMatch(container -> container.isRunning());

            if (kafkaHealthy) {
                report.addComponent("kafka", HealthStatus.HEALTHY, "All consumers running");
            } else {
                report.addComponent("kafka", HealthStatus.DEGRADED, "Some consumers stopped");
            }
        } catch (Exception e) {
            report.addComponent("kafka", HealthStatus.UNHEALTHY,
                    "Error: " + e.getMessage());
        }

        // Circuit breaker health
        Map<String, CircuitBreakerRegistry.CircuitBreakerState> breakerStates = circuitBreakerRegistry.getAllStates();

        long openBreakers = breakerStates.values().stream()
                .filter(state -> state == CircuitBreakerRegistry.CircuitBreakerState.OPEN)
                .count();

        if (openBreakers == 0) {
            report.addComponent("circuit_breakers", HealthStatus.HEALTHY,
                    "All circuits closed");
        } else if (openBreakers < 5) {
            report.addComponent("circuit_breakers", HealthStatus.DEGRADED,
                    openBreakers + " circuits open");
        } else {
            report.addComponent("circuit_breakers", HealthStatus.UNHEALTHY,
                    openBreakers + " circuits open");
        }

        // Determine overall health
        report.calculateOverallHealth();
        this.systemHealth = report.getOverallHealth();

        return report;
    }

    /**
     * Get detailed performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Crawl metrics
        Map<String, Object> crawlMetrics = new HashMap<>();
        crawlMetrics.put("throughput_per_minute", crawlsLastMinute.get());
        crawlMetrics.put("avg_latency_ms", crawlLatencyTimer.mean(TimeUnit.MILLISECONDS));
        crawlMetrics.put("max_latency_ms", crawlLatencyTimer.max(TimeUnit.MILLISECONDS));
        crawlMetrics.put("total_count", crawlLatencyTimer.count());
        metrics.put("crawl", crawlMetrics);

        // Index metrics
        Map<String, Object> indexMetrics = new HashMap<>();
        indexMetrics.put("throughput_per_minute", indexesLastMinute.get());
        indexMetrics.put("avg_latency_ms", indexLatencyTimer.mean(TimeUnit.MILLISECONDS));
        indexMetrics.put("max_latency_ms", indexLatencyTimer.max(TimeUnit.MILLISECONDS));
        indexMetrics.put("total_count", indexLatencyTimer.count());
        indexMetrics.put("total_documents", webPageRepository.count());
        metrics.put("index", indexMetrics);

        // Search metrics
        Map<String, Object> searchMetrics = new HashMap<>();
        searchMetrics.put("avg_latency_ms", searchLatencyTimer.mean(TimeUnit.MILLISECONDS));
        searchMetrics.put("p95_latency_ms",
                searchLatencyTimer.percentile(0.95, TimeUnit.MILLISECONDS));
        searchMetrics.put("p99_latency_ms",
                searchLatencyTimer.percentile(0.99, TimeUnit.MILLISECONDS));
        searchMetrics.put("total_queries", searchLatencyTimer.count());
        metrics.put("search", searchMetrics);

        // Queue metrics
        Map<String, Object> queueMetrics = new HashMap<>();
        queueMetrics.put("pending", crawlUrlRepository.countByStatus(CrawlStatus.PENDING));
        queueMetrics.put("in_progress", crawlUrlRepository.countByStatus(CrawlStatus.IN_PROGRESS));
        queueMetrics.put("completed", crawlUrlRepository.countByStatus(CrawlStatus.COMPLETED));
        queueMetrics.put("failed", crawlUrlRepository.countByStatus(CrawlStatus.FAILED));
        metrics.put("queue", queueMetrics);

        return metrics;
    }

    /**
     * Calculate crawl efficiency (success rate)
     */
    public double getCrawlEfficiency() {
        long completed = crawlUrlRepository.countByStatus(CrawlStatus.COMPLETED);
        long failed = crawlUrlRepository.countByStatus(CrawlStatus.FAILED);
        long total = completed + failed;

        if (total == 0) {
            return 100.0;
        }

        return (double) completed / total * 100.0;
    }

    /**
     * Get slow domains (bottom 10% by crawl speed)
     */
    public List<DomainPerformance> getSlowDomains(int limit) {
        // In production, maintain a separate table for domain-level metrics
        // This is a simplified version

        List<DomainPerformance> slowDomains = new ArrayList<>();

        // Query for domains with high failure rates
        // Placeholder implementation

        return slowDomains;
    }

    /**
     * Check if system should trigger alerts
     */
    public List<Alert> checkAlertConditions() {
        List<Alert> alerts = new ArrayList<>();

        // Alert: High error rate
        double efficiency = getCrawlEfficiency();
        if (efficiency < 80.0) {
            alerts.add(new Alert(
                    AlertLevel.WARNING,
                    "Crawl efficiency below 80%: " + String.format("%.2f%%", efficiency),
                    "crawler.efficiency"));
        }

        // Alert: Queue backlog
        long pending = crawlUrlRepository.countByStatus(CrawlStatus.PENDING);
        if (pending > 10000) {
            alerts.add(new Alert(
                    AlertLevel.WARNING,
                    "Large crawl queue backlog: " + pending + " URLs",
                    "crawler.queue.backlog"));
        }

        // Alert: Slow searches
        double avgSearchLatency = searchLatencyTimer.mean(TimeUnit.MILLISECONDS);
        if (avgSearchLatency > 500) {
            alerts.add(new Alert(
                    AlertLevel.WARNING,
                    "Search latency high: " + String.format("%.2fms", avgSearchLatency),
                    "search.latency"));
        }

        // Alert: Many circuit breakers open
        Map<String, Object> cbStats = circuitBreakerRegistry.getStats();
        long openBreakers = (long) cbStats.getOrDefault("open", 0L);
        if (openBreakers > 10) {
            alerts.add(new Alert(
                    AlertLevel.CRITICAL,
                    "Many circuit breakers open: " + openBreakers,
                    "circuit_breaker.open"));
        }

        return alerts;
    }

    /**
     * Reset throughput counters (scheduled every minute)
     */
    @Scheduled(fixedRate = 60000)
    public void resetThroughputCounters() {
        crawlsLastMinute.set(0);
        indexesLastMinute.set(0);
    }

    /**
     * Periodic health check and alerting
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performHealthCheck() {
        HealthReport report = getHealthReport();

        if (report.getOverallHealth() != HealthStatus.HEALTHY) {
            log.warn("System health degraded: {}", report);
        }

        List<Alert> alerts = checkAlertConditions();
        for (Alert alert : alerts) {
            log.warn("ALERT [{}]: {}", alert.getLevel(), alert.getMessage());

            // In production, send to alerting system (PagerDuty, Slack, etc.)
        }
    }

    // ========================================
    // DATA CLASSES
    // ========================================

    public enum HealthStatus {
        HEALTHY(0),
        DEGRADED(1),
        UNHEALTHY(2);

        private final int severity;

        HealthStatus(int severity) {
            this.severity = severity;
        }

        public int getSeverity() {
            return severity;
        }
    }

    @lombok.Data
    public static class HealthReport {
        private LocalDateTime timestamp;
        private HealthStatus overallHealth;
        private Map<String, ComponentHealth> components = new HashMap<>();

        public void addComponent(String name, HealthStatus status, String details) {
            components.put(name, new ComponentHealth(status, details));
        }

        public void calculateOverallHealth() {
            int maxSeverity = components.values().stream()
                    .mapToInt(c -> c.getStatus().getSeverity())
                    .max()
                    .orElse(0);

            this.overallHealth = Arrays.stream(HealthStatus.values())
                    .filter(s -> s.getSeverity() == maxSeverity)
                    .findFirst()
                    .orElse(HealthStatus.HEALTHY);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ComponentHealth {
        private HealthStatus status;
        private String details;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DomainPerformance {
        private String domain;
        private double avgLatencyMs;
        private double successRate;
        private long totalRequests;
    }

    public enum AlertLevel {
        INFO,
        WARNING,
        CRITICAL
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Alert {
        private AlertLevel level;
        private String message;
        private String metric;
    }
}