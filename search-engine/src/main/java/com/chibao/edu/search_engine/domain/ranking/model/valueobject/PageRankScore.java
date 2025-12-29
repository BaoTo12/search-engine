package com.chibao.edu.search_engine.domain.ranking.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object for PageRank scores.
 */
@Getter
@EqualsAndHashCode
public final class PageRankScore {
    private final double value;

    private PageRankScore(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("PageRank score must be between 0 and 1");
        }
        this.value = value;
    }

    public static PageRankScore of(double value) {
        return new PageRankScore(value);
    }

    public static PageRankScore zero() {
        return new PageRankScore(0.0);
    }

    @Override
    public String toString() {
        return String.format("%.6f", value);
    }
}
