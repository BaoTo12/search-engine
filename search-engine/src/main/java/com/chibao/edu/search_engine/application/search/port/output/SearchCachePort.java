package com.chibao.edu.search_engine.application.search.port.output;

import com.chibao.edu.search_engine.application.search.dto.SearchResponseDTO;

/**
 * Output Port (Interface) for search caching.
 * Defined in application layer, implemented in infrastructure layer.
 * This is the "Port" in Ports & Adapters (Hexagonal Architecture).
 */
public interface SearchCachePort {

    /**
     * Get cached search result.
     * 
     * @param cacheKey Cache key
     * @return Cached response or null if not found
     */
    SearchResponseDTO get(String cacheKey);

    /**
     * Cache search result.
     * 
     * @param cacheKey   Cache key
     * @param response   Response to cache
     * @param ttlMinutes Time to live in minutes
     */
    void put(String cacheKey, SearchResponseDTO response, int ttlMinutes);

    /**
     * Clear all cached search results.
     */
    void clear();

    /**
     * Generate cache key from query parameters.
     * 
     * @param query  Search query
     * @param page   Page number
     * @param size   Page size
     * @param sortBy Sort criteria
     * @return Cache key
     */
    default String generateCacheKey(String query, int page, int size, String sortBy) {
        return String.format("search:%s:%d:%d:%s", query, page, size, sortBy);
    }
}
