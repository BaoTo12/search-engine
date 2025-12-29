package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Elasticsearch repository for web pages.
 */
@Repository
public interface WebPageEsRepository extends ElasticsearchRepository<WebPageEsDocument, String> {
}
