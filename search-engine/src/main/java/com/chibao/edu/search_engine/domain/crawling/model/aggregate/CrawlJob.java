package com.chibao.edu.search_engine.domain.crawling.model.aggregate;

import com.chibao.edu.search_engine.domain.crawling.event.CrawlCompletedEvent;
import com.chibao.edu.search_engine.domain.crawling.event.CrawlFailedEvent;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlDepth;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CrawlJob Aggregate Root.
 * Represents a single URL crawl job in the system.
 */
@Getter
@Builder
public class CrawlJob {

    private String id;
    private Url url;
    private CrawlDepth depth;
    private CrawlStatus status;
    private Double priority;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime crawledAt;

    @Builder.Default
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Mark this crawl job as in progress.
     */
    public void markAsInProgress() {
        if (this.status == CrawlStatus.COMPLETED) {
            throw new IllegalStateException("Cannot restart completed crawl job");
        }
        this.status = CrawlStatus.IN_PROGRESS;
    }

    /**
     * Mark this crawl job as completed.
     */
    public void markAsCompleted() {
        if (this.status != CrawlStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete in-progress crawls");
        }
        this.status = CrawlStatus.COMPLETED;
        this.crawledAt = LocalDateTime.now();
        this.publishEvent(new CrawlCompletedEvent(this.id, this.url.getValue(), this.crawledAt));
    }

    /**
     * Mark this crawl job as failed.
     */
    public void markAsFailed(String errorMessage) {
        this.status = CrawlStatus.FAILED;
        this.retryCount++;
        this.publishEvent(new CrawlFailedEvent(this.id, this.url.getValue(), errorMessage, LocalDateTime.now()));
    }

    /**
     * Check if this job can be retried.
     */
    public boolean canRetry() {
        return this.retryCount < 3 && this.status == CrawlStatus.FAILED;
    }

    /**
     * Increase priority.
     */
    public void increasePriority(double amount) {
        this.priority += amount;
    }

    private void publishEvent(Object event) {
        this.domainEvents.add(event);
    }

    public void clearEvents() {
        this.domainEvents.clear();
    }
}
