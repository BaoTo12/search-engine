package com.chibao.edu.search_engine.presentation.rest.controller;

import com.chibao.edu.search_engine.application.search.dto.SearchRequestDTO;
import com.chibao.edu.search_engine.application.search.dto.SearchResponseDTO;
import com.chibao.edu.search_engine.application.search.usecase.GetSuggestionsUseCase;
import com.chibao.edu.search_engine.application.search.usecase.SearchDocumentsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Presentation Layer: REST Controller for Search endpoints.
 * 
 * Clean Architecture flow:
 * 1. Receives HTTP request
 * 2. Creates application DTO
 * 3. Delegates to Use Case
 * 4. Returns HTTP response
 * 
 * NO BUSINESS LOGIC HERE - only HTTP concerns!
 */
@RestController
@RequestMapping("/api/v2/search") // v2 to not conflict with old controller
@Tag(name = "Search API (Clean Architecture)", description = "Search endpoints following Clean Architecture + DDD")
public class SearchControllerV2 {

    private final SearchDocumentsUseCase searchDocumentsUseCase;
    private final GetSuggestionsUseCase getSuggestionsUseCase;

    public SearchControllerV2(
            SearchDocumentsUseCase searchDocumentsUseCase,
            GetSuggestionsUseCase getSuggestionsUseCase) {
        this.searchDocumentsUseCase = searchDocumentsUseCase;
        this.getSuggestionsUseCase = getSuggestionsUseCase;
    }

    @GetMapping
    @Operation(summary = "Search web pages (Clean Architecture)", description = "Full-text search with clean separation of concerns")
    public ResponseEntity<SearchResponseDTO> search(
            @RequestParam(required = true) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "relevance") String sortBy) {
        // Create application DTO
        SearchRequestDTO request = SearchRequestDTO.builder()
                .query(q)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build();

        // Execute use case
        SearchResponseDTO response = searchDocumentsUseCase.execute(request);

        // Return HTTP response
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Autocomplete suggestions for search queries")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(required = true) String prefix) {
        List<String> suggestions = getSuggestionsUseCase.execute(prefix);
        return ResponseEntity.ok(suggestions);
    }
}
