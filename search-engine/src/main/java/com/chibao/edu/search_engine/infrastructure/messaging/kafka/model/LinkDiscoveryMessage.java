package com.chibao.edu.search_engine.infrastructure.messaging.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message format for newly discovered links published to Kafka.
 * Crawler workers publish these to the 'new-links' topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkDiscoveryMessage {

    /**
     * The newly discovered URL.
     */
    private String url;

    /**
     * The source URL where this link was found.
     */
    private String sourceUrl;

    /**
     * Domain of the discovered URL.
     */
    private String domain;

    /**
     * Anchor text of the link (if available).
     */
    private String anchorText;

    /**
     * Depth in the crawl tree (source depth + 1).
     */
    private Integer depth;

    /**
     * When this link was discovered.
     */
    private LocalDateTime discoveredAt;

    /**
     * Position of the link in the source page (for prioritization).
     */
    private Integer positionInPage;

    /**
     * Whether this link was in the main content or footer/sidebar.
     */
    private Boolean isMainContent;
}
