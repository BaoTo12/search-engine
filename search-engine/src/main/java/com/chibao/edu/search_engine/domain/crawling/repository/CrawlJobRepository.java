package com.chibao.edu.search_engine.domain.crawling.repository;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;

import java.util.List;
import java.util.Optional;

/**
 * Domain Repository Interface for CrawlJob aggregate.
 * Infrastructure layer will implement this.
 */
public interface CrawlJobRepository {

    void save(CrawlJob crawlJob);

    Optional<CrawlJob> findById(String id);

    List<CrawlJob> findByStatus(CrawlStatus status, int limit);

    void delete(CrawlJob crawlJob);
}
