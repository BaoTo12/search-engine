package com.chibao.edu.search_engine.infrastructure.messaging.kafka.consumer;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlDepth;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.domain.crawling.service.UrlNormalizationService;
import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.LinkDiscoveryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer for new-links topic.
 * Processes discovered links and adds them to the crawl queue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkDiscoveryConsumer {

    private final CrawlJobRepository crawlJobRepository;
    private final UrlNormalizationService urlNormalizationService;
    private final ObjectMapper objectMapper;

    private static final int MAX_DEPTH = 3;

    @KafkaListener(topics = "new-links", groupId = "link-processors", concurrency = "4")
    public void consume(String message, Acknowledgment ack) {
        try {
            // Deserialize message
            LinkDiscoveryMessage linkMessage = objectMapper.readValue(message, LinkDiscoveryMessage.class);

            log.debug("Processing discovered link: {}", linkMessage.getUrl());

            // Check depth limit
            if (linkMessage.getDepth() > MAX_DEPTH) {
                log.debug("Link exceeds max depth: {}", linkMessage.getUrl());
                ack.acknowledge();
                return;
            }

            // Normalize URL
            String normalizedUrl;
            try {
                normalizedUrl = urlNormalizationService.normalize(linkMessage.getUrl());
            } catch (Exception e) {
                log.debug("Invalid URL: {}", linkMessage.getUrl());
                ack.acknowledge();
                return;
            }

            Url url = Url.of(normalizedUrl);

            // Check if already in queue
            if (crawlJobRepository.existsByUrl(url)) {
                log.debug("URL already in queue: {}", normalizedUrl);
                ack.acknowledge();
                return;
            }

            // Add to crawl queue
            CrawlJob crawlJob = CrawlJob.builder()
                    .url(url)
                    .depth(CrawlDepth.of(linkMessage.getDepth()))
                    .status(CrawlStatus.PENDING)
                    .priority(calculatePriority(linkMessage))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            crawlJobRepository.save(crawlJob);

            log.info("Added new URL to queue: {}", normalizedUrl);

            // Acknowledge message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing link discovery: {}", e.getMessage(), e);
        }
    }

    private Double calculatePriority(LinkDiscoveryMessage message) {
        double priority = 1.0;

        // Lower priority for deeper links
        priority -= (message.getDepth() * 0.1);

        // Higher priority for main content
        if (Boolean.TRUE.equals(message.getIsMainContent())) {
            priority += 0.5;
        }

        // Higher priority if has good anchor text
        if (message.getAnchorText() != null && !message.getAnchorText().isEmpty()) {
            priority += 0.2;
        }

        return Math.max(0.1, priority); // Minimum priority of 0.1
    }
}
