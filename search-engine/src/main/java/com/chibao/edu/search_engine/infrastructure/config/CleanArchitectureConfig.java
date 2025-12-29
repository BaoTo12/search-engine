package com.chibao.edu.search_engine.infrastructure.config;

import com.chibao.edu.search_engine.application.deduplication.usecase.DetectDuplicateContentUseCase;
import com.chibao.edu.search_engine.application.ranking.usecase.CalculatePageRankUseCase;
import com.chibao.edu.search_engine.domain.crawling.service.RobotsTxtParser;
import com.chibao.edu.search_engine.domain.crawling.service.UrlNormalizationService;
import com.chibao.edu.search_engine.domain.crawling.service.UrlPrioritizationService;
import com.chibao.edu.search_engine.domain.deduplication.service.BloomFilterService;
import com.chibao.edu.search_engine.domain.deduplication.service.SimHashService;
import com.chibao.edu.search_engine.domain.indexing.service.TextProcessingService;
import com.chibao.edu.search_engine.domain.ranking.repository.PageGraphRepository;
import com.chibao.edu.search_engine.domain.ranking.service.PageRankCalculator;
import com.chibao.edu.search_engine.domain.search.service.QueryExpansionService;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.adapter.PageGraphRepositoryJpaAdapter;
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

    // ========== Domain Services ==========

    @Bean
    public UrlNormalizationService urlNormalizationService() {
        return new UrlNormalizationService();
    }

    @Bean
    public UrlPrioritizationService urlPrioritizationService() {
        return new UrlPrioritizationService();
    }

    @Bean
    public RobotsTxtParser robotsTxtParser() {
        return new RobotsTxtParser();
    }

    @Bean
    public TextProcessingService textProcessingService() {
        return new TextProcessingService();
    }

    @Bean
    public PageRankCalculator pageRankCalculator() {
        return new PageRankCalculator();
    }

    @Bean
    public BloomFilterService bloomFilterService() {
        return new BloomFilterService();
    }

    @Bean
    public SimHashService simHashService() {
        return new SimHashService();
    }

    @Bean
    public QueryExpansionService queryExpansionService() {
        return new QueryExpansionService();
    }

    // ========== Repository Interfaces → Adapters ==========

    @Bean
    public PageGraphRepository pageGraphRepository(PageGraphRepositoryJpaAdapter adapter) {
        return adapter;
    }

    // ========== Use Cases ==========

    @Bean
    public CalculatePageRankUseCase calculatePageRankUseCase(
            PageGraphRepository pageGraphRepository,
            PageRankCalculator pageRankCalculator) {
        return new CalculatePageRankUseCase(pageGraphRepository, pageRankCalculator);
    }

    @Bean
    public DetectDuplicateContentUseCase detectDuplicateContentUseCase(
            SimHashService simHashService) {
        return new DetectDuplicateContentUseCase(simHashService);
    }
}
