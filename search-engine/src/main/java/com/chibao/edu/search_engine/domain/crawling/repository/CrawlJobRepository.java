package com.chibao.edu.search_engine.domain.crawling.repository;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CrawlJob aggregate.
 * Defined in domain layer, implemented in infrastructure.
 */
public interface CrawlJobRepository {

    /**
     * Save a crawl job.
     */
    void save(CrawlJob crawlJob);

    /**
     * Find crawl job by ID.
     */
    Optional<CrawlJob> findById(String id);

    /**
     * Find crawl job by URL.
     */
    Optional<CrawlJob> findByUrl(Url url);

    /**
     * Check if a URL already exists in the queue.
     */
    boolean existsByUrl(Url url);

    /**
     * Find pending crawl jobs ready to be processed.
     */
    List<CrawlJob> findPendingJobs(int limit);

    /**
     * Count crawl jobs by status.
     */
    long countByStatus(CrawlStatus status);

    /**
     * Delete a crawl job.
     */
    void delete(CrawlJob crawlJob);
}
