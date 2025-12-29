package com.chibao.edu.search_engine.application.search.dto;

/**
 * Application DTO for search requests.
 * Maps from presentation layer to domain layer.
 */
public record SearchRequestDTO(
        String query,
        Integer page,
        Integer size,
        String sortBy) {
    public SearchRequestDTO {
        // Record compact constructor for validation
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        // Set defaults
        page = page != null ? page : 0;
        size = size != null ? size : 10;
        sortBy = sortBy != null ? sortBy : "relevance";
    }
}
