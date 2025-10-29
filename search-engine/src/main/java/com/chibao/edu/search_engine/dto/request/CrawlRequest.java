package com.chibao.edu.search_engine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRequest {
    private String url;
    private String domain;
    private Integer depth;
    private Integer priority;
    private LocalDateTime timestamp;
}