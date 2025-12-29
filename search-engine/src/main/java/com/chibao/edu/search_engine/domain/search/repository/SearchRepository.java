package com.chibao.edu.search_engine.domain.search.repository;

import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;

import java.util.List;

/**
 * Repository interface for search operations.
 * Defined in domain, implemented in infrastructure.
 */
public interface SearchRepository {

    /**
     * Search for documents matching the query.
     */
    List<SearchResultEntity> search(SearchQuery query, Pagination pagination);

    /**
     * Count total results for a query.
     */
    long countResults(SearchQuery query);
}
