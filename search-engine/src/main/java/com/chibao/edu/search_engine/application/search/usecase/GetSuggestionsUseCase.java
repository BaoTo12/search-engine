package com.chibao.edu.search_engine.application.search.usecase;

import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;

import java.util.List;

/**
 * Use Case: Get Search Suggestions (Autocomplete)
 * Simple use case delegating to domain repository.
 */
public class GetSuggestionsUseCase {

    private final SearchRepository searchRepository;
    private static final int DEFAULT_LIMIT = 5;

    public GetSuggestionsUseCase(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public List<String> execute(String prefix) {
        if (prefix == null || prefix.length() < 2) {
            return List.of();
        }

        // TODO: Implement getSuggestions in SearchRepository
        // For now, return empty list
        return List.of();
    }
}
