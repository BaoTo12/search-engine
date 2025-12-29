package com.chibao.edu.search_engine.application.crawling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for adding seed URLs request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedUrlRequestDTO {
    private List<String> urls;
    private Integer maxDepth;
    private Double priority;
}
