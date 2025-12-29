package com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.DomainMetadataJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for domain_metadata table.
 */
@Repository
public interface DomainMetadataJpaRepository extends JpaRepository<DomainMetadataJpaEntity, Long> {

    /**
     * Find domain metadata by domain name.
     */
    Optional<DomainMetadataJpaEntity> findByDomain(String domain);

    /**
     * Check if domain metadata exists.
     */
    boolean existsByDomain(String domain);
}
