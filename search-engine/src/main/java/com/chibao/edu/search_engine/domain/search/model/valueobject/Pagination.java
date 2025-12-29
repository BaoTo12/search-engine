package com.chibao.edu.search_engine.domain.search.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object for pagination parameters.
 */
@Getter
@EqualsAndHashCode
public final class Pagination {
    private final int page;
    private final int size;

    private Pagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
        this.page = page;
        this.size = size;
    }

    public static Pagination of(int page, int size) {
        return new Pagination(page, size);
    }

    public Pagination nextPage() {
        return new Pagination(this.page + 1, this.size);
    }

    public Pagination previousPage() {
        if (this.page == 0) {
            return this;
        }
        return new Pagination(this.page - 1, this.size);
    }
}
