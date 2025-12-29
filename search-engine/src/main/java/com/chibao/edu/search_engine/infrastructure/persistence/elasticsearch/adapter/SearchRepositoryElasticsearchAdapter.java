package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.adapter;

import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;
import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository.WebPageEsRepository;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Infrastructure Adapter: Implements domain SearchRepository using
 * Elasticsearch.
 * This is the "Adapter" in Ports & Adapters (Hexagonal Architecture).
 * 
 * Responsibilities:
 * - Implement domain repository interface
 * - Convert domain models ↔ infrastructure models (WebPage entity)
 * - Handle Elasticsearch-specific logic
 */
@Component
public class SearchRepositoryElasticsearchAdapter implements SearchRepository {

        private final ElasticsearchOperations elasticsearchOperations;
        private final WebPageEsRepository webPageEsRepository; // Spring Data ES repository

        public SearchRepositoryElasticsearchAdapter(
                        ElasticsearchOperations elasticsearchOperations,
                        WebPageEsRepository webPageEsRepository) {
                this.elasticsearchOperations = elasticsearchOperations;
                this.webPageEsRepository = webPageEsRepository;
        }

        @Override
        public List<SearchResultEntity> search(SearchQuery query, Pagination pagination) {
                // Build Elasticsearch query
                Query boolQuery = QueryBuilders.bool(b -> b
                                .should(QueryBuilders.match(m -> m.field("title").query(query.getValue()).boost(3.0f)))
                                .should(QueryBuilders
                                                .match(m -> m.field("content").query(query.getValue()).boost(1.0f)))
                                .should(QueryBuilders.match(m -> m.field("tokens").query(query.getValue()).boost(2.0f)))
                                .minimumShouldMatch("1"));

                PageRequest pageRequest = PageRequest.of(pagination.getPage(), pagination.getSize());

                NativeQuery searchQuery = new NativeQueryBuilder()
                                .withQuery(boolQuery)
                                .withPageable(pageRequest)
                                .build();

                // Execute search
                SearchHits<WebPageEsDocument> searchHits = elasticsearchOperations.search(searchQuery,
                                WebPageEsDocument.class);

                // Convert infrastructure entities to domain entities
                return searchHits.getSearchHits().stream()
                                .map(this::toDomainEntity)
                                .collect(Collectors.toList());
        }

        @Override
        public long countResults(SearchQuery query) {
                Query boolQuery = QueryBuilders.bool(b -> b
                                .should(QueryBuilders.match(m -> m.field("title").query(query.getValue())))
                                .should(QueryBuilders.match(m -> m.field("content").query(query.getValue())))
                                .minimumShouldMatch("1"));

                NativeQuery countQuery = new NativeQueryBuilder()
                                .withQuery(boolQuery)
                                .build();

                SearchHits<WebPageEsDocument> hits = elasticsearchOperations.search(countQuery,
                                WebPageEsDocument.class);
                return hits.getTotalHits();
        }

        @Override
        public List<String> getSuggestions(String prefix, int limit) {
                Query prefixQuery = QueryBuilders.matchPhrasePrefix(m -> m.field("title").query(prefix));

                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(prefixQuery)
                                .withPageable(PageRequest.of(0, limit))
                                .build();

                SearchHits<WebPageEsDocument> hits = elasticsearchOperations.search(query, WebPageEsDocument.class);

                return hits.getSearchHits().stream()
                                .map(hit -> hit.getContent().getTitle())
                                .distinct()
                                .limit(limit)
                                .collect(Collectors.toList());
        }

        /**
         * Convert infrastructure document (WebPageEsDocument) to domain entity
         * (SearchResultEntity).
         * This mapping keeps domain independent of infrastructure.
         */
        private SearchResultEntity toDomainEntity(SearchHit<WebPageEsDocument> hit) {
                WebPageEsDocument page = hit.getContent();
                double normalizedScore = hit.getScore() > 0 ? Math.min(hit.getScore() / 10.0, 1.0) : 0.0;

                return SearchResultEntity.create(
                                page.getUrl(),
                                page.getTitle(),
                                page.getSnippet() != null ? page.getSnippet() : "",
                                normalizedScore,
                                page.getIndexedAt());
        }
}
