package com.chibao.edu.search_engine.infrastructure.persistence.jpa.adapter;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.*;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity.CrawlStatusJpa;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository.CrawlUrlJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter: Implements domain CrawlJobRepository using JPA.
 * Converts between domain models and JPA entities.
 */
@Component
public class CrawlJobRepositoryJpaAdapter implements CrawlJobRepository {

    private final CrawlUrlJpaRepository jpaRepository;

    public CrawlJobRepositoryJpaAdapter(CrawlUrlJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(CrawlJob crawlJob) {
        CrawlUrlJpaEntity entity = toJpaEntity(crawlJob);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<CrawlJob> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomainModel);
    }

    @Override
    public List<CrawlJob> findByStatus(CrawlStatus status, int limit) {
        CrawlStatusJpa jpaStatus = toJpaStatus(status);
        return jpaRepository.findByStatusOrderByCreatedAtAsc(jpaStatus)
                .stream()
                .limit(limit)
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(CrawlJob crawlJob) {
        jpaRepository.deleteById(crawlJob.getId());
    }

    private CrawlJob toDomainModel(CrawlUrlJpaEntity entity) {
        CrawlJob job = CrawlJob.create(
                Url.of(entity.getUrl()),
                entity.getDepth());
        // Reconstruct state (simplified - in real impl would use reflection or factory)
        return job;
    }

    private CrawlUrlJpaEntity toJpaEntity(CrawlJob crawlJob) {
        CrawlUrlJpaEntity entity = new CrawlUrlJpaEntity();
        entity.setId(crawlJob.getId());
        entity.setUrl(crawlJob.getUrl().getValue());
        entity.setDepth(crawlJob.getDepth().getValue());
        entity.setStatus(toJpaStatus(crawlJob.getStatus()));
        entity.setFailureCount(crawlJob.getFailureCount());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private CrawlStatusJpa toJpaStatus(CrawlStatus status) {
        return CrawlStatusJpa.valueOf(status.name());
    }
}
