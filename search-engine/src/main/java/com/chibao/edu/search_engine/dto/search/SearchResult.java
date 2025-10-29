package com.chibao.edu.search_engine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String url;
    private String title;
    private String snippet;
    private Double score;
    private LocalDateTime lastCrawled;
}
