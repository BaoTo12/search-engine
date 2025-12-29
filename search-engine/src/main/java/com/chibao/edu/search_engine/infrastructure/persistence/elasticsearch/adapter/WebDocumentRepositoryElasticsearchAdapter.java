package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.adapter;

import com.chibao.edu.search_engine.domain.indexing.model.aggregate.WebDocument;
import com.chibao.edu.search_engine.domain.indexing.repository.WebDocumentRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository.WebPageEsRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WebDocumentRepositoryElasticsearchAdapter implements WebDocumentRepository {

    private final WebPageEsRepository esRepository;

    public WebDocumentRepositoryElasticsearchAdapter(WebPageEsRepository esRepository) {
        this.esRepository = esRepository;
    }

    @Override
    public void index(WebDocument document) {
        WebPageEsDocument esDoc = toEsDocument(document);
        esRepository.save(esDoc);
    }

    @Override
    public Optional<WebDocument> findByUrl(String url) {
        // Simplified - would need custom query
        return Optional.empty();
    }

    @Override
    public void delete(String id) {
        esRepository.deleteById(id);
    }

    private WebPageEsDocument toEsDocument(WebDocument doc) {
        WebPageEsDocument esDoc = new WebPageEsDocument();
        esDoc.setId(doc.getId());
        esDoc.setUrl(doc.getUrl());
        esDoc.setTitle(doc.getTitle());
        esDoc.setContent(doc.getContent());
        esDoc.setTokens(doc.getTokens());
        esDoc.setIndexedAt(doc.getIndexedAt());
        return esDoc;
    }
}
