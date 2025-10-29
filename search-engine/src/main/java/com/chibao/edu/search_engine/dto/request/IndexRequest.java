package com.chibao.edu.search_engine.dto.request;

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
public class IndexRequest {
    private String url;
    private String title;
    private String content;
    private List<String> outboundLinks;
    private String domain;
    private Integer depth;
    private LocalDateTime crawledAt;
}

