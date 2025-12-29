package com.chibao.edu.search_engine.application.crawling.usecase;

import com.chibao.edu.search_engine.application.crawling.dto.CrawlStatusResponseDTO;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for getting crawl status statistics.
 */
@Service
@RequiredArgsConstructor
public class GetCrawlStatusUseCase {

    private final CrawlJobRepository crawlJobRepository;

    public CrawlStatusResponseDTO execute() {
        long pending = crawlJobRepository.countByStatus(CrawlStatus.PENDING);
        long inProgress = crawlJobRepository.countByStatus(CrawlStatus.IN_PROGRESS);
        long completed = crawlJobRepository.countByStatus(CrawlStatus.COMPLETED);
        long failed = crawlJobRepository.countByStatus(CrawlStatus.FAILED);
        long total = pending + inProgress + completed + failed;

        String status = inProgress > 0 ? "RUNNING" : "IDLE";

        return CrawlStatusResponseDTO.builder()
                .totalUrls(total)
                .pendingUrls(pending)
                .inProgressUrls(inProgress)
                .completedUrls(completed)
                .failedUrls(failed)
                .crawlRate(0.0) // TODO: Calculate actual crawl rate
                .status(status)
                .build();
    }
}
