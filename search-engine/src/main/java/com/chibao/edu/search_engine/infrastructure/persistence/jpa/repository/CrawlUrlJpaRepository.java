package com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity.CrawlStatusJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawlUrlJpaRepository extends JpaRepository<CrawlUrlJpaEntity, String> {
    List<CrawlUrlJpaEntity> findByStatusOrderByCreatedAtAsc(CrawlStatusJpa status);
}
