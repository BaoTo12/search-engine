package com.chibao.edu.search_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity storing PageRank scores for web pages.
 * PageRank is calculated periodically based on the link graph.
 */
@Entity
@Table(name = "page_ranks", indexes = {
        @Index(name = "idx_rank_score", columnList = "rank_score DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, unique = true, length = 2048)
    private String url;

    @Column(name = "rank_score", nullable = false)
    private Double rankScore;

    @Column(name = "inbound_links")
    private Integer inboundLinks;

    @Column(name = "outbound_links")
    private Integer outboundLinks;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastCalculated = LocalDateTime.now();
    }
}
