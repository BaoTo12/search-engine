package com.chibao.edu.search_engine.domain.crawling.event;

import java.time.LocalDateTime;

public record CrawlCompletedEvent(
        String crawlJobId,
        String url,
        LocalDateTime occurredAt) {
    public CrawlCompletedEvent(String crawlJobId, String url) {
        this(crawlJobId, url, LocalDateTime.now());
    }
}
