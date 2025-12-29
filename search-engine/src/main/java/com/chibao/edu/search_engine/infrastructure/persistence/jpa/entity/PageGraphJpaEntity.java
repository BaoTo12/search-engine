package com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for page_graph table.
 */
@Entity
@Table(name = "page_graph")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageGraphJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2048)
    private String url;

    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;

    @Column(name = "pagerank_score", nullable = false)
    private Double pagerankScore;

    @Column(name = "previous_score", nullable = false)
    private Double previousScore;

    @Column(name = "inbound_links_count", nullable = false)
    private Integer inboundLinksCount;

    @Column(name = "outbound_links_count", nullable = false)
    private Integer outboundLinksCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (pagerankScore == null) {
            pagerankScore = 0.0;
        }
        if (previousScore == null) {
            previousScore = 0.0;
        }
        if (inboundLinksCount == null) {
            inboundLinksCount = 0;
        }
        if (outboundLinksCount == null) {
            outboundLinksCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
