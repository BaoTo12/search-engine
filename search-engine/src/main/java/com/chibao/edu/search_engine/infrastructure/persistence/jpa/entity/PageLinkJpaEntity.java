package com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for page_links table.
 */
@Entity
@Table(name = "page_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageLinkJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_page_id", nullable = false)
    private Long sourcePageId;

    @Column(name = "target_page_id", nullable = false)
    private Long targetPageId;

    @Column(name = "anchor_text", columnDefinition = "TEXT")
    private String anchorText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
