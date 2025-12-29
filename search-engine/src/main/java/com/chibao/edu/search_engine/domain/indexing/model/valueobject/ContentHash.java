package com.chibao.edu.search_engine.domain.indexing.model.valueobject;

import java.util.Objects;

public final class ContentHash {
    private final long value;

    private ContentHash(long value) {
        this.value = value;
    }

    public static ContentHash of(long value) {
        return new ContentHash(value);
    }

    public long getValue() {
        return value;
    }

    public int hammingDistance(ContentHash other) {
        return Long.bitCount(this.value ^ other.value);
    }

    public boolean isSimilarTo(ContentHash other, int threshold) {
        return hammingDistance(other) <= threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ContentHash))
            return false;
        ContentHash that = (ContentHash) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Long.toHexString(value);
    }
}
