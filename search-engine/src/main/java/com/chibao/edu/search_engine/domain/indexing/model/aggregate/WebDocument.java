package com.chibao.edu.search_engine.domain.indexing.model.aggregate;

import com.chibao.edu.search_engine.domain.indexing.model.valueobject.ContentHash;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
public class WebDocument {
    private final String id;
    private final String url;
    private final String title;
    private final String content;
    private final ContentHash contentHash;
    private final List<String> tokens;
    private final LocalDateTime indexedAt;

    private WebDocument(String id, String url, String title, String content,
            ContentHash contentHash, List<String> tokens) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.content = content;
        this.contentHash = contentHash;
        this.tokens = tokens;
        this.indexedAt = LocalDateTime.now();
    }

    public static WebDocument create(String url, String title, String content,
            long contentHash, List<String> tokens) {
        return new WebDocument(
                UUID.randomUUID().toString(),
                url,
                title,
                content,
                ContentHash.of(contentHash),
                tokens);
    }

}
