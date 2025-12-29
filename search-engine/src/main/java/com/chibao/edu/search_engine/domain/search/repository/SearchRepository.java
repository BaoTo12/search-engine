package com.chibao.edu.search_engine.domain.search.repository;

import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;

import java.util.List;

/**
 * Domain Repository Interface (Port).
 * Domain layer defines the contract, infrastructure implements it.
 * NO DEPENDENCIES ON INFRASTRUCTURE!
 */
public interface SearchRepository {

    /**
     * Search documents matching the query.
     * 
     * @param query      Search query (value object)
     * @param pagination Pagination parameters (value object)
     * @return List of search results ordered by relevance
     */
    List<SearchResultEntity> search(SearchQuery query, Pagination pagination);

    /**
     * Get total count of results for a query.
     * 
     * @param query Search query
     * @return Total number of matching documents
     */
    long countResults(SearchQuery query);

    /**
     * Get search suggestions (autocomplete).
     * 
     * @param prefix Query prefix
     * @param limit  Maximum number of suggestions
     * @return List of suggested queries
     */
    List<String> getSuggestions(String prefix, int limit);
}
