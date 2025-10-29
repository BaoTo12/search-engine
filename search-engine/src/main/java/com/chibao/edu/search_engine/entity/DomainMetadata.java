package com.chibao.edu.search_engine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "domain_metadata")
public class DomainMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    private LocalDateTime lastCrawlTime;

    @Column(nullable = false)
    private Long crawlDelayMs; // Politeness delay

    @Column(nullable = false)
    private Integer maxConcurrentRequests;

    private String robotsTxtContent;

    @Column(nullable = false)
    private Boolean isBlocked;

    private Integer totalPagesCrawled;

    private Integer totalFailures;
}
