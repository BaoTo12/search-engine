package com.chibao.edu.search_engine.domain.indexing.repository;

import com.chibao.edu.search_engine.domain.indexing.model.aggregate.WebDocument;
import java.util.Optional;

public interface WebDocumentRepository {
    void index(WebDocument document);

    Optional<WebDocument> findByUrl(String url);

    void delete(String id);
}
