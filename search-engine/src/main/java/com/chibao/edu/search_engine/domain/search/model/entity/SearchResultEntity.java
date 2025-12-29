package com.chibao.edu.search_engine.domain.search.model.entity;

import lombok.Builder;
import lombok.Data;

/**
 * Search result entity representing a single search result.
 */
@Data
@Builder
public class SearchResultEntity {
    private String url;
    private String title;
    private String snippet;
    private Double relevanceScore;
    private Double pagerankScore;
    private String language;
}
