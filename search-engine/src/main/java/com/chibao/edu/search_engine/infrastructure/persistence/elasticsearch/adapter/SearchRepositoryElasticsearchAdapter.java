package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.adapter;

import com.chibao.edu.search_engine.domain.search.model.entity.SearchResultEntity;
import com.chibao.edu.search_engine.domain.search.model.valueobject.Pagination;
import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchQuery;
import com.chibao.edu.search_engine.domain.search.repository.SearchRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository.WebPageEsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch adapter implementing SearchRepository interface.
 * Simplified version using Spring Data Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchRepositoryElasticsearchAdapter implements SearchRepository {

        private final WebPageEsRepository webPageRepository;

        @Override
        public List<SearchResultEntity> search(SearchQuery searchQuery, Pagination pagination) {
                try {
                        // Use Spring Data Elasticsearch findAll for now
                        // TODO: Implement proper full-text search with custom queries
                        PageRequest pageRequest = PageRequest.of(pagination.getPage(), pagination.getSize());
                        Page<WebPageEsDocument> results = webPageRepository.findAll(pageRequest);

                        return results.getContent().stream()
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
                        return webPageRepository.count();
                } catch (Exception e) {
                        log.error("Error counting results: {}", e.getMessage());
                        return 0;
                }
        }

        private SearchResultEntity toSearchResultEntity(WebPageEsDocument doc) {
                return SearchResultEntity.builder()
                                .url(doc.getUrl())
                                .title(doc.getTitle() != null ? doc.getTitle() : "Untitled")
                                .snippet(generateSnippet(doc.getContent()))
                                .relevanceScore(1.0) // TODO: Calculate actual relevance score
                                .pagerankScore(doc.getPagerankScore() != null ? doc.getPagerankScore() : 0.0)
                                .language(doc.getLanguage())
                                .build();
        }

        private String generateSnippet(String content) {
                if (content == null || content.isEmpty()) {
                        return "";
                }

                int snippetLength = Math.min(200, content.length());
                String snippet = content.substring(0, snippetLength);

                int lastSpace = snippet.lastIndexOf(' ');
                if (lastSpace > 100) {
                        snippet = snippet.substring(0, lastSpace);
                }

                return snippet + "...";
        }
}
