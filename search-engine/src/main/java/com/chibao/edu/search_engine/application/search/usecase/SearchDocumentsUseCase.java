package com.chibao.edu.search_engine.application.search.usecase;

import com.chibao.edu.search_engine.application.search.dto.SearchRequestDTO;
import com.chibao.edu.search_engine.application.search.dto.SearchResponseDTO;
import com.chibao.edu.search_engine.application.search.dto.SearchResponseDTO.SearchResultDTO;
import com.chibao.edu.search_engine.application.search.port.output.SearchCachePort;
import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;
import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case: Search Documents
 * Application layer orchestrates domain logic and infrastructure.
 * 
 * This is a TRUE use case - contains application-specific business logic.
 */
public class SearchDocumentsUseCase {

    private final SearchRepository searchRepository; // Domain interface
    private final SearchCachePort cachePort; // Application port

    private static final int CACHE_TTL_MINUTES = 30;

    public SearchDocumentsUseCase(
            SearchRepository searchRepository,
            SearchCachePort cachePort) {
        this.searchRepository = searchRepository;
        this.cachePort = cachePort;
    }

    /**
     * Execute the search use case.
     * 
     * Flow:
     * 1. Check cache
     * 2. If miss, query domain repository
     * 3. Map domain entities to DTOs
     * 4. Cache result
     * 5. Return response
     */
    public SearchResponseDTO execute(SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // Generate cache key
        String cacheKey = cachePort.generateCacheKey(
                request.getQuery(),
                request.getPage(),
                request.getSize(),
                request.getSortBy());

        // Check cache
        SearchResponseDTO cachedResult = cachePort.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Create domain value objects
        SearchQuery query = SearchQuery.of(request.getQuery());
        Pagination pagination = Pagination.of(request.getPage(), request.getSize());

        // Execute domain repository query
        List<SearchResultEntity> domainResults = searchRepository.search(query, pagination);
        long totalResults = searchRepository.countResults(query);

        // Map domain entities to application DTOs
        List<SearchResultDTO> resultDTOs = domainResults.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        long executionTime = System.currentTimeMillis() - startTime;

        // Build response
        SearchResponseDTO response = SearchResponseDTO.builder()
                .query(request.getQuery())
                .totalResults(totalResults)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages((int) Math.ceil((double) totalResults / request.getSize()))
                .searchTimeMs(executionTime)
                .results(resultDTOs)
                .build();

        // Cache result
        cachePort.put(cacheKey, response, CACHE_TTL_MINUTES);

        return response;
    }

    private SearchResultDTO mapToDTO(SearchResultEntity entity) {
        return SearchResultDTO.builder()
                .url(entity.getUrl())
                .title(entity.getTitle())
                .snippet(entity.getSnippet())
                .relevanceScore(entity.getRelevanceScore())
                .pagerankScore(entity.getPagerankScore())
                .language(entity.getLanguage())
                .build();
    }
}
