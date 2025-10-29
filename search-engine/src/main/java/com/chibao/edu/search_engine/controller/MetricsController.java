package com.chibao.edu.search_engine.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics API", description = "Application metrics and monitoring")
public class MetricsController {

    private final MeterRegistry meterRegistry;

    @GetMapping
    @Operation(summary = "Get application metrics", description = "Retrieve Prometheus-compatible metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        meterRegistry.getMeters().forEach(meter -> {
            String name = meter.getId().getName();

            meter.measure().forEach(measurement -> {
                metrics.put(name + "." + measurement.getStatistic().name().toLowerCase(),
                        measurement.getValue());
            });
        });

        return metrics;
    }

    @GetMapping("/crawler")
    @Operation(summary = "Get crawler metrics", description = "Retrieve crawler-specific metrics")
    public Map<String, Object> getCrawlerMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("success_count",
                meterRegistry.counter("crawler.success").count());
        metrics.put("failure_count",
                meterRegistry.counter("crawler.failure").count());

        return metrics;
    }

    @GetMapping("/indexer")
    @Operation(summary = "Get indexer metrics", description = "Retrieve indexer-specific metrics")
    public Map<String, Object> getIndexerMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("indexed_count",
                meterRegistry.counter("indexer.indexed").count());

        return metrics;
    }
}