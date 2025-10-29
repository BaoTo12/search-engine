package com.chibao.edu.search_engine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private String query;
    private Long totalResults;
    private Integer page;
    private Integer size;
    private List<SearchResult> results;
    private Long executionTimeMs;
}