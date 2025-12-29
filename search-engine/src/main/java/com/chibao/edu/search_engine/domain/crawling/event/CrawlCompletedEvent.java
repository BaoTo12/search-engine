package com.chibao.edu.search_engine.domain.crawling.event;

import java.time.LocalDateTime;

/**
 * Domain event fired when a crawl is completed successfully.
 */
public record CrawlCompletedEvent(
        String crawlJobId,
        String url,
        LocalDateTime occurredAt) {
}
