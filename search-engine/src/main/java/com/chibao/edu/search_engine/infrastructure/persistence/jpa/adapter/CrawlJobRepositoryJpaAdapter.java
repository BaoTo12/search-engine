package com.chibao.edu.search_engine.infrastructure.persistence.jpa.adapter;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlDepth;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.CrawlUrlJpaEntity;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository.CrawlUrlJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing CrawlJobRepository interface.
 * Maps between domain model (CrawlJob) and infrastructure (JPA entities).
 */
@Component
@RequiredArgsConstructor
public class CrawlJobRepositoryJpaAdapter implements CrawlJobRepository {

    private final CrawlUrlJpaRepository jpaRepository;

    @Override
    public void save(CrawlJob crawlJob) {
        CrawlUrlJpaEntity entity = toEntity(crawlJob);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<CrawlJob> findById(String id) {
        try {
            Long jpaId = Long.parseLong(id);
            return jpaRepository.findById(jpaId)
                    .map(this::toDomain);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CrawlJob> findByUrl(Url url) {
        String hash = calculateHash(url.getValue());
        return jpaRepository.findByUrlHash(hash)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUrl(Url url) {
        String hash = calculateHash(url.getValue());
        return jpaRepository.existsByUrlHash(hash);
    }

    @Override
    public List<CrawlJob> findPendingJobs(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return jpaRepository.findReadyToCrawl(LocalDateTime.now(), pageRequest)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(CrawlStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public void delete(CrawlJob crawlJob) {
        try {
            Long jpaId = Long.parseLong(crawlJob.getId());
            jpaRepository.deleteById(jpaId);
        } catch (NumberFormatException e) {
            // Invalid ID, ignore
        }
    }

    // =========== Mapping Methods ===========

    private CrawlJob toDomain(CrawlUrlJpaEntity entity) {
        return CrawlJob.builder()
                .id(entity.getId().toString())
                .url(Url.of(entity.getUrl()))
                .depth(CrawlDepth.of(entity.getDepth()))
                .status(CrawlStatus.valueOf(entity.getStatus()))
                .priority(entity.getPriority())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CrawlUrlJpaEntity toEntity(CrawlJob domain) {
        CrawlUrlJpaEntity.CrawlUrlJpaEntityBuilder builder = CrawlUrlJpaEntity.builder()
                .url(domain.getUrl().getValue())
                .normalizedUrl(domain.getUrl().getValue()) // Already normalized
                .domain(extractDomain(domain.getUrl().getValue()))
                .urlHash(calculateHash(domain.getUrl().getValue()))
                .status(domain.getStatus().name())
                .priority(domain.getPriority())
                .depth(domain.getDepth().getValue())
                .maxDepth(3) // Default max depth
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now())
                .retryCount(domain.getRetryCount() != null ? domain.getRetryCount() : 0)
                .maxRetries(3);

        // Only set ID if it exists and is valid
        if (domain.getId() != null && !domain.getId().isEmpty()) {
            try {
                builder.id(Long.parseLong(domain.getId()));
            } catch (NumberFormatException e) {
                // New entity, no ID yet
            }
        }

        return builder.build();
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String calculateHash(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
