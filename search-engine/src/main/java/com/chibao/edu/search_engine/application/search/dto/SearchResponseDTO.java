package com.chibao.edu.search_engine.application.search.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application DTO for search responses.
 * Maps from domain layer to presentation layer.
 */
public record SearchResponseDTO(
        String query,
        long totalResults,
        int page,
        int size,
        List<SearchResultDTO> results,
        long executionTimeMs) {
    public record SearchResultDTO(
            String url,
            String title,
            String snippet,
            double score,
            LocalDateTime lastCrawled) {
    }
}
