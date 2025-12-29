package com.chibao.edu.search_engine.domain.search.model.valueobject;

import java.util.Objects;

/**
 * Value Object representing a search score (relevance).
 * Immutable - ensures score is within valid range.
 */
public final class SearchScore {
    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 1.0;

    private final double value;

    private SearchScore(double value) {
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "Search score must be between " + MIN_SCORE + " and " + MAX_SCORE);
        }
        this.value = value;
    }

    public static SearchScore of(double value) {
        return new SearchScore(value);
    }

    public static SearchScore zero() {
        return new SearchScore(0.0);
    }

    public static SearchScore max() {
        return new SearchScore(1.0);
    }

    public double getValue() {
        return value;
    }

    public boolean isHigherThan(SearchScore other) {
        return this.value > other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SearchScore))
            return false;
        SearchScore that = (SearchScore) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("%.3f", value);
    }
}
