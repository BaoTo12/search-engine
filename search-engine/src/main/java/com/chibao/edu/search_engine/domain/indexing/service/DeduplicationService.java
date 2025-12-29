package com.chibao.edu.search_engine.domain.indexing.service;

import com.chibao.edu.search_engine.domain.indexing.model.valueobject.ContentHash;

public interface DeduplicationService {
    ContentHash computeHash(String content);

    boolean isDuplicate(ContentHash hash, int threshold);
}
