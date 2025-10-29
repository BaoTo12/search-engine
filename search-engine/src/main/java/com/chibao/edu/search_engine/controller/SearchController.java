package com.chibao.edu.search_engine.controller;

import com.chibao.edu.search_engine.dto.search.SearchRequest;
import com.chibao.edu.search_engine.dto.search.SearchResponse;
import com.chibao.edu.search_engine.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search API", description = "Endpoints for searching indexed web pages")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search web pages", description = "Perform full-text search across indexed pages")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = true) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "relevance") String sortBy
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(q)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build();

        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get autocomplete suggestions for search queries")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(required = true) String prefix
    ) {
        List<String> suggestions = searchService.getSuggestions(prefix);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear search cache", description = "Clear all cached search results")
    public ResponseEntity<Void> clearCache() {
        searchService.clearCache();
        return ResponseEntity.noContent().build();
    }
}