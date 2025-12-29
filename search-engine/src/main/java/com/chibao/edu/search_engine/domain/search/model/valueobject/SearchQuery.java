package com.chibao.edu.search_engine.domain.search.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object for search queries.
 */
@Getter
@EqualsAndHashCode
public final class SearchQuery {
    private final String value;

    private SearchQuery(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        if (value.length() > 500) {
            throw new IllegalArgumentException("Search query too long");
        }
        this.value = normalize(value);
    }

    public static SearchQuery of(String value) {
        return new SearchQuery(value);
    }

    private String normalize(String query) {
        // Remove extra whitespace
        return query.trim().replaceAll("\\s+", " ");
    }

    @Override
    public String toString() {
        return value;
    }
}
