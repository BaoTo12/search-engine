package com.chibao.edu.search_engine.repository;

import com.chibao.edu.search_engine.entity.DomainMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomainMetadataRepository extends JpaRepository<DomainMetadata, Long> {
    Optional<DomainMetadata> findByDomain(String domain);
}
