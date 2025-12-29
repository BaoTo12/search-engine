package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.adapter;

import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;
import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch adapter implementing SearchRepository interface.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchRepositoryElasticsearchAdapter implements SearchRepository {

        private final ElasticsearchOperations elasticsearchOperations;

        @Override
        public List<SearchResultEntity> search(SearchQuery searchQuery, Pagination pagination) {
                try {
                        // Build Elasticsearch query
                        Query query = NativeQuery.builder()
                                        .withQuery(q -> q
                                                        .multiMatch(m -> m
                                                                        .query(searchQuery.getValue())
                                                                        .fields("title^3", "metaDescription^2",
                                                                                        "content")
                                                                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                                                        .fuzziness("AUTO")))
                                        .withPageable(PageRequest.of(
                                                        pagination.getPage(),
                                                        pagination.getSize()))
                                        .build();

                        // Execute search
                        SearchHits<WebPageEsDocument> searchHits = elasticsearchOperations.search(
                                        query,
                                        WebPageEsDocument.class);

                        // Map to domain entities
                        return searchHits.getSearchHits().stream()
                                        .map(this::toSearchResultEntity)
                                        .collect(Collectors.toList());

                } catch (Exception e) {
                        log.error("Error searching Elasticsearch: {}", e.getMessage(), e);
                        return List.of();
                }
        }

        @Override
        public long countResults(SearchQuery searchQuery) {
                try {
                        Query query = NativeQuery.builder()
                                        .withQuery(q -> q
                                                        .multiMatch(m -> m
                                                                        .query(searchQuery.getValue())
                                                                        .fields("title", "metaDescription", "content")))
                                        .build();

                        SearchHits<WebPageEsDocument> searchHits = elasticsearchOperations.search(
                                        query,
                                        WebPageEsDocument.class);

                        return searchHits.getTotalHits();

                } catch (Exception e) {
                        log.error("Error counting results: {}", e.getMessage());
                        return 0;
                }
        }

        private SearchResultEntity toSearchResultEntity(SearchHit<WebPageEsDocument> hit) {
                WebPageEsDocument doc = hit.getContent();

                return SearchResultEntity.builder()
                                .url(doc.getUrl())
                                .title(doc.getTitle() != null ? doc.getTitle() : "Untitled")
                                .snippet(generateSnippet(doc.getContent()))
                                .relevanceScore(hit.getScore())
                                .pagerankScore(doc.getPagerankScore() != null ? doc.getPagerankScore() : 0.0)
                                .language(doc.getLanguage())
                                .build();
        }

        private String generateSnippet(String content) {
                if (content == null || content.isEmpty()) {
                        return "";
                }

                // Simple snippet generation - take first 200 chars
                int snippetLength = Math.min(200, content.length());
                String snippet = content.substring(0, snippetLength);

                // Try to end at a word boundary
                int lastSpace = snippet.lastIndexOf(' ');
                if (lastSpace > 100) {
                        snippet = snippet.substring(0, lastSpace);
                }

                return snippet + "...";
        }
}
