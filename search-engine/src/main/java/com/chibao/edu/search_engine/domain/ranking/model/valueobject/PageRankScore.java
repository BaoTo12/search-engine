package com.chibao.edu.search_engine.domain.ranking.model.valueobject;

import java.util.Objects;

public final class PageRankScore {
    private final double value;

    private PageRankScore(double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("PageRank must be between 0 and 1");
        }
        this.value = value;
    }

    public static PageRankScore of(double value) {
        return new PageRankScore(value);
    }

    public static PageRankScore zero() {
        return new PageRankScore(0.0);
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PageRankScore))
            return false;
        PageRankScore that = (PageRankScore) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("%.6f", value);
    }
}
