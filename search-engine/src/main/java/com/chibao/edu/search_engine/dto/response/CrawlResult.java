package com.chibao.edu.search_engine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResult {
    private String url;
    private String domain;
    private Integer depth;
    private String title;
    private String content;
    private List<String> outboundLinks;
    private Integer httpStatusCode;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime crawledAt;
}