package com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.PageLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for page_links table.
 */
@Repository
public interface PageLinkJpaRepository extends JpaRepository<PageLinkJpaEntity, Long> {

    @Query("SELECT pl FROM PageLinkJpaEntity pl WHERE pl.sourcePageId = :pageId")
    List<PageLinkJpaEntity> findBySourcePageId(Long pageId);

    @Query("SELECT pl FROM PageLinkJpaEntity pl WHERE pl.targetPageId = :pageId")
    List<PageLinkJpaEntity> findByTargetPageId(Long pageId);
}
