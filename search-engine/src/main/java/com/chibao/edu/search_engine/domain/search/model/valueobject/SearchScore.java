package com.chibao.edu.search_engine.domain.search.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object for search scores.
 */
@Getter
@EqualsAndHashCode
public final class SearchScore {
    private final double value;

    private SearchScore(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        this.value = value;
    }

    public static SearchScore of(double value) {
        return new SearchScore(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
