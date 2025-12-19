package com.chibao.edu.search_engine.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Expanded query with synonyms, corrections, and metadata.
 */
@Data
@Builder
public class ExpandedQuery {
    private String original;
    private String corrected;
    private List<String> synonyms;
    private List<String> entities;
    private String intent; // tutorial, documentation, question, etc.
    private double confidence;
}
