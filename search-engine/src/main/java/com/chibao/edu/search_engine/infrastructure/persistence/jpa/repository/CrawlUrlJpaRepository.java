package com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for crawl_urls table.
 */
@Repository
public interface CrawlUrlJpaRepository extends JpaRepository<CrawlUrlJpaEntity, Long> {

    /**
     * Find by URL hash (for deduplication).
     */
    Optional<CrawlUrlJpaEntity> findByUrlHash(String urlHash);

    /**
     * Check if URL exists by hash.
     */
    boolean existsByUrlHash(String urlHash);

    /**
     * Find URLs ready to be crawled (PENDING status, scheduled time passed).
     */
    @Query("""
                SELECT c FROM CrawlUrlJpaEntity c
                WHERE c.status = 'PENDING'
                AND (c.scheduledAt IS NULL OR c.scheduledAt <= :now)
                AND c.retryCount < c.maxRetries
                ORDER BY c.priority DESC, c.createdAt ASC
            """)
    Page<CrawlUrlJpaEntity> findReadyToCrawl(
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * Find URLs for recrawling (completed but next_crawl_at time has passed).
     */
    @Query("""
                SELECT c FROM CrawlUrlJpaEntity c
                WHERE c.status = 'COMPLETED'
                AND c.nextCrawlAt IS NOT NULL
                AND c.nextCrawlAt <= :now
                ORDER BY c.priority DESC
            """)
    Page<CrawlUrlJpaEntity> findReadyForRecrawl(
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * Find all URLs for a specific domain.
     */
    List<CrawlUrlJpaEntity> findByDomain(String domain);

    /**
     * Count URLs by status.
     */
    long countByStatus(String status);

    /**
     * Find failed URLs that can be retried.
     */
    @Query("""
                SELECT c FROM CrawlUrlJpaEntity c
                WHERE c.status = 'FAILED'
                AND c.retryCount < c.maxRetries
                ORDER BY c.priority DESC, c.createdAt ASC
            """)
    Page<CrawlUrlJpaEntity> findFailedToRetry(Pageable pageable);

    /**
     * Update status in bulk.
     */
    @Modifying
    @Query("UPDATE CrawlUrlJpaEntity c SET c.status = :newStatus WHERE c.status = :oldStatus")
    int updateStatus(
            @Param("oldStatus") String oldStatus,
            @Param("newStatus") String newStatus);

    /**
     * Find oldest pending URLs for a domain (for fairness).
     */
    @Query("""
                SELECT c FROM CrawlUrlJpaEntity c
                WHERE c.status = 'PENDING'
                AND c.domain = :domain
                ORDER BY c.createdAt ASC
            """)
    Page<CrawlUrlJpaEntity> findOldestPendingByDomain(
            @Param("domain") String domain,
            Pageable pageable);
}
