package com.chibao.edu.search_engine.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized metrics service for application monitoring.
 * Uses Micrometer for metrics collection (compatible with Prometheus).
 */
@Component
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // ========== Crawling Metrics ==========

    public void recordCrawlSuccess(String domain) {
        Counter.builder("crawler.requests.success")
                .tag("domain", domain)
                .description("Number of successful crawl requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordCrawlFailure(String domain, String errorType) {
        Counter.builder("crawler.requests.failure")
                .tag("domain", domain)
                .tag("error_type", errorType)
                .description("Number of failed crawl requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordCrawlDuration(String domain, long durationMs) {
        Timer.builder("crawler.request.duration")
                .tag("domain", domain)
                .description("Duration of crawl requests in milliseconds")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordPagesIndexed() {
        Counter.builder("indexer.pages.indexed")
                .description("Total number of pages indexed")
                .register(meterRegistry)
                .increment();
    }

    // ========== Search Metrics ==========

    public void recordSearchQuery(String queryType) {
        Counter.builder("search.queries.total")
                .tag("type", queryType)
                .description("Total number of search queries")
                .register(meterRegistry)
                .increment();
    }

    public void recordSearchDuration(long durationMs) {
        Timer.builder("search.query.duration")
                .description("Search query duration in milliseconds")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordSearchResults(int resultCount) {
        meterRegistry.summary("search.results.count")
                .record(resultCount);
    }

    // ========== PageRank Metrics ==========

    public void recordPageRankCalculation(int pageCount, int iterationCount, long durationMs) {
        Counter.builder("pagerank.calculations.total")
                .description("Total number of PageRank calculations")
                .register(meterRegistry)
                .increment();

        meterRegistry.gauge("pagerank.last.page.count", pageCount);
        meterRegistry.gauge("pagerank.last.iteration.count", iterationCount);

        Timer.builder("pagerank.calculation.duration")
                .description("PageRank calculation duration in milliseconds")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ========== Deduplication Metrics ==========

    public void recordBloomFilterCheck(boolean isDuplicate) {
        Counter.builder("bloom.filter.checks")
                .tag("result", isDuplicate ? "duplicate" : "new")
                .description("Bloom filter duplicate checks")
                .register(meterRegistry)
                .increment();
    }

    public void recordSimHashComparison(boolean isNearDuplicate) {
        Counter.builder("simhash.comparisons")
                .tag("result", isNearDuplicate ? "duplicate" : "unique")
                .description("SimHash near-duplicate comparisons")
                .register(meterRegistry)
                .increment();
    }

    // ========== Resilience Metrics ==========

    public void recordCircuitBreakerState(String resourceName, String state) {
        meterRegistry.gauge("circuit.breaker.state",
                java.util.Arrays.asList(
                        io.micrometer.core.instrument.Tag.of("resource", resourceName),
                        io.micrometer.core.instrument.Tag.of("state", state)),
                state.equals("CLOSED") ? 0 : state.equals("HALF_OPEN") ? 1 : 2);
    }

    public void recordRetryAttempt(String operation, int attemptNumber) {
        Counter.builder("retry.attempts")
                .tag("operation", operation)
                .tag("attempt", String.valueOf(attemptNumber))
                .description("Retry attempts per operation")
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitHit(String domain) {
        Counter.builder("rate.limit.hits")
                .tag("domain", domain)
                .description("Number of rate limit hits")
                .register(meterRegistry)
                .increment();
    }

    // ========== Queue Metrics ==========

    public void recordQueueSize(String queueName, int size) {
        meterRegistry.gauge("queue.size",
                java.util.Collections.singletonList(io.micrometer.core.instrument.Tag.of("queue", queueName)),
                size);
    }

    public void recordKafkaMessagePublished(String topic) {
        Counter.builder("kafka.messages.published")
                .tag("topic", topic)
                .description("Messages published to Kafka")
                .register(meterRegistry)
                .increment();
    }

    public void recordKafkaMessageConsumed(String topic) {
        Counter.builder("kafka.messages.consumed")
                .tag("topic", topic)
                .description("Messages consumed from Kafka")
                .register(meterRegistry)
                .increment();
    }
}
