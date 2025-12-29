package com.chibao.edu.search_engine.infrastructure.messaging.kafka.consumer;

import com.chibao.edu.search_engine.application.crawling.usecase.CrawlPageUseCase;
import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.CrawlRequestMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for crawl-requests topic.
 * Workers listen to this topic and execute crawl jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlRequestConsumer {

    private final CrawlPageUseCase crawlPageUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "crawl-requests", groupId = "crawler-workers", concurrency = "4")
    public void consume(String message, Acknowledgment ack) {
        try {
            // Deserialize message
            CrawlRequestMessage crawlRequest = objectMapper.readValue(message, CrawlRequestMessage.class);

            log.info("Received crawl request for: {}", crawlRequest.getUrl());

            // Execute crawl
            crawlPageUseCase.execute(crawlRequest.getCrawlJobId());

            // Acknowledge message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing crawl request: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried or go to DLQ
        }
    }
}
