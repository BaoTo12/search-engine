package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository;

import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface WebPageEsRepository extends ElasticsearchRepository<WebPageEsDocument, String> {
}
