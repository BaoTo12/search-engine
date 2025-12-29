package com.chibao.edu.search_engine.application.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Enhanced search response DTO with metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDTO {
        private String query;
        private Long totalResults;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Long searchTimeMs;
        private List<SearchResultDTO> results;
        private List<String> suggestions; // "Did you mean?" suggestions

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SearchResultDTO {
                private String url;
                private String title;
                private String snippet;
                private Double relevanceScore;
                private Double pagerankScore;
                private String language;
                private String crawledAt;
                private List<String> highlightedTerms;
        }
}
