package com.chibao.edu.search_engine.domain.search.model.valueobject;

import java.util.Objects;

/**
 * Value Object representing pagination parameters.
 * Immutable - ensures valid pagination values.
 */
public final class Pagination {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int MIN_SIZE = 1;

    private final int page;
    private final int size;

    private Pagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Page size must be between " + MIN_SIZE + " and " + MAX_SIZE);
        }
        this.page = page;
        this.size = size;
    }

    public static Pagination of(int page, int size) {
        return new Pagination(page, size);
    }

    public static Pagination defaultPagination() {
        return new Pagination(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return page * size;
    }

    public Pagination nextPage() {
        return new Pagination(page + 1, size);
    }

    public Pagination previousPage() {
        if (page == 0) {
            return this;
        }
        return new Pagination(page - 1, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Pagination))
            return false;
        Pagination that = (Pagination) o;
        return page == that.page && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size);
    }

    @Override
    public String toString() {
        return String.format("Page %d, Size %d", page, size);
    }
}
