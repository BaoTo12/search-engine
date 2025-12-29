package com.chibao.edu.search_engine.application.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhanced search request DTO with filters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDTO {
    private String query;
    private Integer page;
    private Integer size;
    private String sortBy; // relevance, date, pagerank
    private String language;
    private String domain; // Filter by specific domain
    private String dateFrom; // ISO date
    private String dateTo; // ISO date
    private Double minContentQuality;
}
