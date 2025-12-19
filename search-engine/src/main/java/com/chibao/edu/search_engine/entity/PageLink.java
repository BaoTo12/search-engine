package com.chibao.edu.search_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a link between two web pages.
 * Used for building the link graph needed for PageRank calculation.
 */
@Entity
@Table(name = "page_links", indexes = {
        @Index(name = "idx_source_url", columnList = "source_url"),
        @Index(name = "idx_target_url", columnList = "target_url")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "source_url", "target_url" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "anchor_text", columnDefinition = "TEXT")
    private String anchorText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
