package com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for crawl_urls table.
 * Maps to the database representation of URLs in the crawl queue.
 */
@Entity
@Table(name = "crawl_urls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlUrlJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Column(name = "normalized_url", nullable = false, length = 2048)
    private String normalizedUrl;

    @Column(nullable = false)
    private String domain;

    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;

    // Crawl status
    @Column(nullable = false, length = 50)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED, BLOCKED

    @Column(nullable = false)
    private Double priority;

    @Column(nullable = false)
    private Integer depth;

    @Column(name = "max_depth", nullable = false)
    private Integer maxDepth;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Column(name = "next_crawl_at")
    private LocalDateTime nextCrawlAt;

    // Retry and error handling
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    // Metadata
    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "anchor_text", columnDefinition = "TEXT")
    private String anchorText;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (priority == null) {
            priority = 0.0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
        if (depth == null) {
            depth = 0;
        }
        if (maxDepth == null) {
            maxDepth = 3;
        }
    }
}
