package com.chibao.edu.search_engine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkDiscoveryMessage {
    private List<String> urls;
    private String sourceDomain;
    private Integer sourceDepth;
}