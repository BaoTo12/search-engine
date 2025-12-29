package com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_urls")
public class CrawlUrlJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Integer depth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlStatusJpa status;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public CrawlStatusJpa getStatus() {
        return status;
    }

    public void setStatus(CrawlStatusJpa status) {
        this.status = status;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(LocalDateTime lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum CrawlStatusJpa {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, BLOCKED
    }
}
