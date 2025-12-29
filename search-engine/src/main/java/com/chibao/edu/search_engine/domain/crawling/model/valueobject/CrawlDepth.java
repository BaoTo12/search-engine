package com.chibao.edu.search_engine.domain.crawling.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object representing crawl depth.
 */
@Getter
@EqualsAndHashCode
public final class CrawlDepth {

    private final int value;

    private CrawlDepth(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Crawl depth cannot be negative");
        }
        this.value = value;
    }

    public static CrawlDepth of(int value) {
        return new CrawlDepth(value);
    }

    public CrawlDepth increment() {
        return new CrawlDepth(this.value + 1);
    }

    public boolean isGreaterThan(int maxDepth) {
        return this.value > maxDepth;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
