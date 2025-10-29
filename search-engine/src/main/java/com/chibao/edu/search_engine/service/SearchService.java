package com.chibao.edu.search_engine.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.chibao.edu.search_engine.dto.search.SearchRequest;
import com.chibao.edu.search_engine.dto.search.SearchResponse;
import com.chibao.edu.search_engine.dto.search.SearchResult;
import com.chibao.edu.search_engine.entity.WebPage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "search:";
    private static final int CACHE_TTL_MINUTES = 30;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Perform search query
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        // Validate and normalize request
        String query = normalizeQuery(request.getQuery());
        int page = Math.max(0, request.getPage() != null ? request.getPage() : 0);
        int size = Math.min(MAX_PAGE_SIZE, request.getSize() != null ? request.getSize() : DEFAULT_PAGE_SIZE);
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "relevance";

        log.info("Searching for: '{}' (page={}, size={})", query, page, size);

        // Check cache
        String cacheKey = generateCacheKey(query, page, size, sortBy);
        SearchResponse cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.debug("Returning cached result for: {}", query);
            return cachedResult;
        }

        // Build Elasticsearch query
        NativeQuery searchQuery = buildSearchQuery(query, page, size, sortBy);

        // Execute search
        SearchHits<WebPage> searchHits = elasticsearchOperations.search(searchQuery, WebPage.class);

        // Convert to response
        List<SearchResult> results = searchHits.getSearchHits().stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());

        long executionTime = System.currentTimeMillis() - startTime;

        SearchResponse response = SearchResponse.builder()
                .query(query)
                .totalResults(searchHits.getTotalHits())
                .page(page)
                .size(size)
                .results(results)
                .executionTimeMs(executionTime)
                .build();

        // Cache result
        cacheResult(cacheKey, response);

        log.info("Search completed in {}ms, found {} results", executionTime, searchHits.getTotalHits());

        return response;
    }

    /**
     * Build Elasticsearch native search query (Spring Data Elasticsearch 5.x style)
     */
    private NativeQuery buildSearchQuery(String query, int page, int size, String sortBy) {
        Query boolQuery = QueryBuilders.bool(b -> b
                .should(QueryBuilders.match(m -> m.field("title").query(query).boost(3.0f)))
                .should(QueryBuilders.match(m -> m.field("content").query(query).boost(1.0f)))
                .should(QueryBuilders.match(m -> m.field("tokens").query(query).boost(2.0f)))
                .minimumShouldMatch("1")
        );

        Pageable pageable = PageRequest.of(page, size);

        var queryBuilder = new NativeQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(pageable);

        switch (sortBy.toLowerCase()) {
            case "date" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "lastCrawled"));
            case "pagerank" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "pageRank"));
            default -> {}
        }

        return queryBuilder.build();
    }

    /**
     * Convert SearchHit to SearchResult DTO
     */
    private SearchResult convertToSearchResult(SearchHit<WebPage> hit) {
        WebPage page = hit.getContent();
        return SearchResult.builder()
                .url(page.getUrl())
                .title(page.getTitle())
                .snippet(page.getSnippet())
                .score((double) hit.getScore())
                .lastCrawled(page.getLastCrawled())
                .build();
    }

    /**
     * Normalize search query
     */
    private String normalizeQuery(String query) {
        if (query == null) return "";
        query = query.trim().replaceAll("\\s+", " ");
        if (query.length() > 500) query = query.substring(0, 500);
        return query;
    }

    /**
     * Generate cache key for query
     */
    private String generateCacheKey(String query, int page, int size, String sortBy) {
        return String.format("%s%s:%d:%d:%s", CACHE_PREFIX, query, page, size, sortBy);
    }

    /**
     * Get cached search result from Redis
     */
    private SearchResponse getCachedResult(String cacheKey) {
        try {
            return (SearchResponse) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to get cached result: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * Cache search result in Redis
     */
    private void cacheResult(String cacheKey, SearchResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache result: {}", cacheKey, e);
        }
    }

    /**
     * Get search suggestions (autocomplete)
     */
    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.length() < 2) return List.of();

        try {
            Query prefixQuery = QueryBuilders.matchPhrasePrefix(m -> m.field("title").query(prefix));

            var query = new NativeQueryBuilder()
                    .withQuery(prefixQuery)
                    .withPageable(PageRequest.of(0, 5))
                    .build();

            SearchHits<WebPage> hits = elasticsearchOperations.search(query, WebPage.class);

            return hits.getSearchHits().stream()
                    .map(hit -> hit.getContent().getTitle())
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get suggestions for: {}", prefix, e);
            return List.of();
        }
    }

    /**
     * Clear search cache
     */
    public void clearCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
        }
    }
}