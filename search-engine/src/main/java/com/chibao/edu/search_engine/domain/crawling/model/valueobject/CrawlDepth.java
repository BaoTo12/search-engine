package com.chibao.edu.search_engine.domain.crawling.model.valueobject;

import java.util.Objects;

public final class CrawlDepth {
    private static final int MAX_DEPTH = 10;
    private final int value;

    private CrawlDepth(int value) {
        if (value < 0 || value > MAX_DEPTH) {
            throw new IllegalArgumentException("Crawl depth must be between 0 and " + MAX_DEPTH);
        }
        this.value = value;
    }

    public static CrawlDepth of(int value) {
        return new CrawlDepth(value);
    }

    public static CrawlDepth zero() {
        return new CrawlDepth(0);
    }

    public int getValue() {
        return value;
    }

    public CrawlDepth increment() {
        if (value >= MAX_DEPTH) {
            return this;
        }
        return new CrawlDepth(value + 1);
    }

    public boolean isMaxDepth() {
        return value >= MAX_DEPTH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CrawlDepth))
            return false;
        CrawlDepth that = (CrawlDepth) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
