package com.chibao.edu.search_engine.infrastructure.messaging.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message format for crawl requests published to Kafka.
 * Scheduler publishes these to the 'crawl-requests' topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRequestMessage {

    /**
     * Unique identifier for this crawl job.
     */
    private String crawlJobId;

    /**
     * URL to be crawled.
     */
    private String url;

    /**
     * Domain of the URL (for domain-based partitioning).
     */
    private String domain;

    /**
     * Current depth in the crawl tree (0 = seed URL).
     */
    private Integer depth;

    /**
     * Maximum depth allowed for this crawl job.
     */
    private Integer maxDepth;

    /**
     * Priority score for this URL (higher = more important).
     */
    private Double priority;

    /**
     * Timestamp when this request was created.
     */
    private LocalDateTime timestamp;

    /**
     * Number of retry attempts for this URL.
     */
    private Integer retryCount;
}
