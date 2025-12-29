package com.chibao.edu.search_engine.application.crawling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for crawl status response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlStatusResponseDTO {
    private Long totalUrls;
    private Long pendingUrls;
    private Long inProgressUrls;
    private Long completedUrls;
    private Long failedUrls;
    private Double crawlRate; // URLs per minute
    private String status; // IDLE, RUNNING, PAUSED
}
