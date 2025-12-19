package com.chibao.edu.search_engine.repository;

import com.chibao.edu.search_engine.entity.PageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageLinkRepository extends JpaRepository<PageLink, Long> {

    /**
     * Find all outbound links from a source URL.
     */
    List<PageLink> findBySourceUrl(String sourceUrl);

    /**
     * Find all inbound links to a target URL.
     */
    List<PageLink> findByTargetUrl(String targetUrl);

    /**
     * Count outbound links from a URL.
     */
    long countBySourceUrl(String sourceUrl);

    /**
     * Count inbound links to a URL.
     */
    long countByTargetUrl(String targetUrl);

    /**
     * Get all unique source URLs (for PageRank calculation).
     */
    @Query("SELECT DISTINCT pl.sourceUrl FROM PageLink pl")
    List<String> findAllSourceUrls();

    /**
     * Get all unique target URLs.
     */
    @Query("SELECT DISTINCT pl.targetUrl FROM PageLink pl")
    List<String> findAllTargetUrls();

    /**
     * Check if a link already exists.
     */
    boolean existsBySourceUrlAndTargetUrl(String sourceUrl, String targetUrl);
}
