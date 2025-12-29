package com.chibao.edu.search_engine.infrastructure.scheduling;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.CrawlRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that dispatches pending crawl jobs to Kafka.
 * Runs periodically to populate the crawl-requests topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlSchedulerJob {

    private final CrawlJobRepository crawlJobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int BATCH_SIZE = 100; // Dispatch 100 URLs per run

    /**
     * Runs every 10 seconds to dispatch pending crawl jobs.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void dispatchCrawlJobs() {
        try {
            // Find pending crawl jobs
            List<CrawlJob> pendingJobs = crawlJobRepository.findPendingJobs(BATCH_SIZE);

            if (pendingJobs.isEmpty()) {
                return; // No work to do
            }

            log.info("Dispatching {} crawl jobs to Kafka", pendingJobs.size());

            for (CrawlJob job : pendingJobs) {
                try {
                    // Create Kafka message
                    CrawlRequestMessage message = CrawlRequestMessage.builder()
                            .crawlJobId(job.getId())
                            .url(job.getUrl().getValue())
                            .domain(extractDomain(job.getUrl().getValue()))
                            .depth(job.getDepth().getValue())
                            .maxDepth(3) // Default max depth
                            .priority(job.getPriority())
                            .timestamp(LocalDateTime.now())
                            .retryCount(job.getRetryCount())
                            .build();

                    // Send to Kafka (partitioned by domain for politeness)
                    kafkaTemplate.send("crawl-requests", message.getDomain(), message);

                    log.debug("Dispatched crawl job: {}", job.getUrl().getValue());

                } catch (Exception e) {
                    log.error("Error dispatching crawl job {}: {}", job.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in crawl scheduler: {}", e.getMessage(), e);
        }
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
