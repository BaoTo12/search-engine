package com.chibao.edu.search_engine.infrastructure.config;

import com.chibao.edu.search_engine.application.search.port.output.SearchCachePort;
import com.chibao.edu.search_engine.application.search.usecase.GetSuggestionsUseCase;
import com.chibao.edu.search_engine.application.search.usecase.SearchDocumentsUseCase;
import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;
import com.chibao.edu.search_engine.infrastructure.cache.redis.adapter.SearchCacheRedisAdapter;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.adapter.SearchRepositoryElasticsearchAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clean Architecture Configuration.
 * Wires together:
 * - Domain interfaces → Infrastructure implementations (Dependency Inversion)
 * - Application ports → Infrastructure adapters (Hexagonal Architecture)
 * - Use cases with their dependencies
 * This is the ONLY place where we know about concrete implementations.
 */
@Configuration
public class CleanArchitectureConfig {

    /**
     * Wire domain repository interface to Elasticsearch adapter.
     * This is Dependency Inversion Principle in action!
     */
    @Bean
    public SearchRepository searchRepository(SearchRepositoryElasticsearchAdapter adapter) {
        return adapter; // Return interface, inject implementation
    }

    /**
     * Wire application port to Redis adapter.
     */
    @Bean
    public SearchCachePort searchCachePort(SearchCacheRedisAdapter adapter) {
        return adapter;
    }

    /**
     * Create SearchDocumentsUseCase with its dependencies.
     */
    @Bean
    public SearchDocumentsUseCase searchDocumentsUseCase(
            SearchRepository searchRepository,
            SearchCachePort searchCachePort) {
        return new SearchDocumentsUseCase(searchRepository, searchCachePort);
    }

    /**
     * Create GetSuggestionsUseCase with its dependencies.
     */
    @Bean
    public GetSuggestionsUseCase getSuggestionsUseCase(SearchRepository searchRepository) {
        return new GetSuggestionsUseCase(searchRepository);
    }
}
