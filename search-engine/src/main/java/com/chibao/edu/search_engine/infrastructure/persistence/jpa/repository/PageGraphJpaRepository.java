package com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.PageGraphJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for page_graph table.
 */
@Repository
public interface PageGraphJpaRepository extends JpaRepository<PageGraphJpaEntity, Long> {

    Optional<PageGraphJpaEntity> findByUrl(String url);

    Optional<PageGraphJpaEntity> findByUrlHash(String urlHash);

    @Query("SELECT p FROM PageGraphJpaEntity p ORDER BY p.pagerankScore DESC")
    List<PageGraphJpaEntity> findTopByPagerankScore(org.springframework.data.domain.Pageable pageable);
}
