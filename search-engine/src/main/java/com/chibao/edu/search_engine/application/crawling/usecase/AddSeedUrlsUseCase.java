package com.chibao.edu.search_engine.application.crawling.usecase;

import com.chibao.edu.search_engine.application.crawling.dto.SeedUrlRequestDTO;
import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlDepth;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.domain.crawling.service.UrlNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Use case for adding seed URLs to the crawl queue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddSeedUrlsUseCase {

    private final CrawlJobRepository crawlJobRepository;
    private final UrlNormalizationService urlNormalizationService;

    public void execute(SeedUrlRequestDTO request) {
        log.info("Adding {} seed URLs to crawl queue", request.getUrls().size());

        for (String urlString : request.getUrls()) {
            try {
                // Normalize URL
                String normalizedUrl = urlNormalizationService.normalize(urlString);

                // Create Url value object
                Url url = Url.of(normalizedUrl);

                // Check if already exists
                if (crawlJobRepository.existsByUrl(url)) {
                    log.debug("URL already in queue: {}", normalizedUrl);
                    continue;
                }

                // Create crawl job
                CrawlJob crawlJob = CrawlJob.builder()
                        .url(url)
                        .depth(CrawlDepth.of(0)) // Seed URLs are depth 0
                        .status(CrawlStatus.PENDING)
                        .priority(request.getPriority() != null ? request.getPriority() : 1.0)
                        .retryCount(0)
                        .createdAt(LocalDateTime.now())
                        .build();

                // Save to repository
                crawlJobRepository.save(crawlJob);

                log.info("Added seed URL to queue: {}", normalizedUrl);

            } catch (IllegalArgumentException e) {
                log.warn("Invalid URL: {} - {}", urlString, e.getMessage());
            } catch (Exception e) {
                log.error("Error adding seed URL {}: {}", urlString, e.getMessage());
            }
        }
    }
}
