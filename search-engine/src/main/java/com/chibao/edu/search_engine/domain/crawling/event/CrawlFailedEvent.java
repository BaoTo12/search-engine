package com.chibao.edu.search_engine.domain.crawling.event;

import java.time.LocalDateTime;

public record CrawlFailedEvent(
        String crawlJobId,
        String url,
        String reason,
        LocalDateTime occurredAt) {
    public CrawlFailedEvent(String crawlJobId, String url, String reason) {
        this(crawlJobId, url, reason, LocalDateTime.now());
    }
}
