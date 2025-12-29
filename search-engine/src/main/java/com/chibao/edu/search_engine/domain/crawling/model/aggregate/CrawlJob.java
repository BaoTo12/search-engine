package com.chibao.edu.search_engine.domain.crawling.model.aggregate;

import com.chibao.edu.search_engine.domain.crawling.model.valueobject.*;
import com.chibao.edu.search_engine.domain.crawling.event.CrawlCompletedEvent;
import com.chibao.edu.search_engine.domain.crawling.event.CrawlFailedEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root for Crawling domain.
 * Encapsulates all business logic related to a crawl job.
 */
public class CrawlJob {
    private final String id;
    private final Url url;
    private final CrawlDepth depth;
    private CrawlStatus status;
    private int failureCount;
    private LocalDateTime lastAttempt;
    private final List<Object> domainEvents;

    private CrawlJob(String id, Url url, CrawlDepth depth, CrawlStatus status) {
        this.id = id;
        this.url = url;
        this.depth = depth;
        this.status = status;
        this.failureCount = 0;
        this.domainEvents = new ArrayList<>();
    }

    public static CrawlJob create(Url url, int depth) {
        return new CrawlJob(
                UUID.randomUUID().toString(),
                url,
                CrawlDepth.of(depth),
                CrawlStatus.PENDING);
    }

    public void markAsInProgress() {
        if (this.status != CrawlStatus.PENDING) {
            throw new IllegalStateException("Can only start pending crawls");
        }
        this.status = CrawlStatus.IN_PROGRESS;
        this.lastAttempt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        if (this.status != CrawlStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete in-progress crawls");
        }
        this.status = CrawlStatus.COMPLETED;
        this.domainEvents.add(new CrawlCompletedEvent(this.id, this.url.getValue()));
    }

    public void markAsFailed(String reason) {
        this.status = CrawlStatus.FAILED;
        this.failureCount++;
        this.domainEvents.add(new CrawlFailedEvent(this.id, this.url.getValue(), reason));
    }

    public boolean canRetry() {
        return this.failureCount < 3;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Url getUrl() {
        return url;
    }

    public CrawlDepth getDepth() {
        return depth;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
