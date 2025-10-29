package com.chibao.edu.search_engine.repository;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.entity.CrawlUrl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlUrlRepository extends JpaRepository<CrawlUrl, Long> {

    boolean existsByUrlHash(String urlHash);

    Optional<CrawlUrl> findByUrlHash(String urlHash);

    List<CrawlUrl> findByStatusOrderByPriorityDesc(CrawlStatus status, Pageable pageable);

    List<CrawlUrl> findByStatusAndLastCrawlAttemptBefore(
            CrawlStatus status,
            LocalDateTime cutoffTime,
            Pageable pageable
    );

    List<CrawlUrl> findByDomainAndStatus(String domain, CrawlStatus status);

    Long countByStatus(CrawlStatus status);
}