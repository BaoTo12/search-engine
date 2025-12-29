package com.chibao.edu.search_engine.domain.crawling.event;

import java.time.LocalDateTime;

/**
 * Domain event fired when a crawl fails.
 */
public record CrawlFailedEvent(
        String crawlJobId,
        String url,
        String errorMessage,
        LocalDateTime occurredAt) {
}
