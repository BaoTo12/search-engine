package com.chibao.edu.search_engine.infrastructure.messaging.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message format for crawled page content published to Kafka.
 * Crawler workers publish these to the 'pages' topic for indexing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageContentMessage {

    /**
     * URL of the crawled page.
     */
    private String url;

    /**
     * Page title (from <title> tag).
     */
    private String title;

    /**
     * Meta description (from <meta name="description"> tag).
     */
    private String metaDescription;

    /**
     * Plain text content extracted from the page.
     */
    private String content;

    /**
     * HTML content (optional, for advanced processing).
     */
    private String htmlContent;

    /**
     * List of outbound URLs found on this page.
     */
    private List<String> outboundLinks;

    /**
     * HTTP status code (200, 404, etc.).
     */
    private Integer statusCode;

    /**
     * Content type (text/html, application/pdf, etc.).
     */
    private String contentType;

    /**
     * Language detected (en, es, fr, etc.).
     */
    private String language;

    /**
     * When this page was crawled.
     */
    private LocalDateTime crawledAt;

    /**
     * Size of the content in bytes.
     */
    private Long contentSizeBytes;

    /**
     * Hash of the content for deduplication.
     */
    private String contentHash;
}
