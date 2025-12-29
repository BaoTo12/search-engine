package com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for domain_metadata table.
 * Stores robots.txt, sitemap, and rate limiting information.
 */
@Entity
@Table(name = "domain_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainMetadataJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    // Robots.txt information
    @Column(name = "robots_txt_content", columnDefinition = "TEXT")
    private String robotsTxtContent;

    @Column(name = "robots_txt_fetched_at")
    private LocalDateTime robotsTxtFetchedAt;

    @Column(name = "robots_txt_expires_at")
    private LocalDateTime robotsTxtExpiresAt;

    @Column(name = "crawl_delay_seconds")
    private Integer crawlDelaySeconds;

    @Column(name = "disallowed_paths", columnDefinition = "TEXT[]")
    private String[] disallowedPaths;

    // Sitemap information
    @Column(name = "sitemap_urls", columnDefinition = "TEXT[]")
    private String[] sitemapUrls;

    @Column(name = "sitemap_fetched_at")
    private LocalDateTime sitemapFetchedAt;

    // Crawl statistics
    @Column(name = "total_urls_discovered")
    private Integer totalUrlsDiscovered;

    @Column(name = "total_urls_crawled")
    private Integer totalUrlsCrawled;

    @Column(name = "total_urls_failed")
    private Integer totalUrlsFailed;

    @Column(name = "average_response_time_ms")
    private Integer averageResponseTimeMs;

    // Rate limiting
    @Column(name = "last_crawl_at")
    private LocalDateTime lastCrawlAt;

    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    // Domain quality metrics
    @Column(name = "domain_authority_score")
    private Double domainAuthorityScore;

    @Column(name = "average_content_quality")
    private Double averageContentQuality;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (crawlDelaySeconds == null) {
            crawlDelaySeconds = 1;
        }
        if (requestsPerMinute == null) {
            requestsPerMinute = 10;
        }
        if (totalUrlsDiscovered == null) {
            totalUrlsDiscovered = 0;
        }
        if (totalUrlsCrawled == null) {
            totalUrlsCrawled = 0;
        }
        if (totalUrlsFailed == null) {
            totalUrlsFailed = 0;
        }
        if (domainAuthorityScore == null) {
            domainAuthorityScore = 0.0;
        }
        if (averageContentQuality == null) {
            averageContentQuality = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
