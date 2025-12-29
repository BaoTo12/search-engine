package com.chibao.edu.search_engine.domain.search.model.valueobject;

import java.util.Objects;

/**
 * Value Object representing a search query.
 * Immutable - encapsulates validation logic.
 */
public final class SearchQuery {
    private static final int MAX_LENGTH = 500;
    private final String value;

    private SearchQuery(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        String normalizedValue = normalize(value);

        if (normalizedValue.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Search query too long: max " + MAX_LENGTH + " characters");
        }

        this.value = normalizedValue;
    }

    public static SearchQuery of(String value) {
        return new SearchQuery(value);
    }

    private String normalize(String query) {
        // Normalize: trim and reduce multiple spaces to single space
        return query.trim().replaceAll("\\s+", " ");
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SearchQuery))
            return false;
        SearchQuery that = (SearchQuery) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
