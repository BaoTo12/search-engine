package com.chibao.edu.search_engine.entity;

import com.chibao.edu.search_engine.common.CrawlStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "crawl_urls", indexes = {
        @Index(name = "idx_url_hash", columnList = "urlHash", unique = true),
        @Index(name = "idx_domain", columnList = "domain"),
        @Index(name = "idx_status", columnList = "status")
})
public class CrawlUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, unique = true, length = 64)
    private String urlHash; // SHA-256 hash

    @Column(nullable = false, length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlStatus status;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false)
    private Integer priority; // 1-10, higher = more important

    private LocalDateTime lastCrawlAttempt;

    private LocalDateTime lastSuccessfulCrawl;

    private Integer failureCount;

    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now(); // FIX: Set updatedAt to prevent NULL constraint violation
        if (priority == null)
            priority = 5;
        if (failureCount == null)
            failureCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
