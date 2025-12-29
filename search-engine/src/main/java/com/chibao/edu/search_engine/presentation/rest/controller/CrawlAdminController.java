package com.chibao.edu.search_engine.presentation.rest.controller;

import com.chibao.edu.search_engine.application.crawling.dto.CrawlStatusResponseDTO;
import com.chibao.edu.search_engine.application.crawling.dto.SeedUrlRequestDTO;
import com.chibao.edu.search_engine.application.crawling.usecase.AddSeedUrlsUseCase;
import com.chibao.edu.search_engine.application.crawling.usecase.GetCrawlStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for crawl administration.
 * Provides endpoints to manage crawler.
 */
@RestController
@RequestMapping("/api/v1/admin/crawl")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Crawl Admin", description = "Endpoints for managing the web crawler")
public class CrawlAdminController {

    private final AddSeedUrlsUseCase addSeedUrlsUseCase;
    private final GetCrawlStatusUseCase getCrawlStatusUseCase;

    @PostMapping("/seeds")
    @Operation(summary = "Add seed URLs", description = "Add seed URLs to start crawling")
    public ResponseEntity<String> addSeedUrls(@RequestBody SeedUrlRequestDTO request) {
        log.info("Received request to add {} seed URLs", request.getUrls().size());

        addSeedUrlsUseCase.execute(request);

        return ResponseEntity.ok("Seed URLs added successfully");
    }

    @GetMapping("/status")
    @Operation(summary = "Get crawl status", description = "Get current crawling statistics")
    public ResponseEntity<CrawlStatusResponseDTO> getCrawlStatus() {
        CrawlStatusResponseDTO status = getCrawlStatusUseCase.execute();
        return ResponseEntity.ok(status);
    }
}
