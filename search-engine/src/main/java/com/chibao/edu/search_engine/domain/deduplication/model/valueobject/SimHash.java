package com.chibao.edu.search_engine.domain.deduplication.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object for SimHash values.
 */
@Getter
@EqualsAndHashCode
public final class SimHash {
    private final long value;

    private SimHash(long value) {
        this.value = value;
    }

    public static SimHash of(long value) {
        return new SimHash(value);
    }

    public static SimHash fromContent(String content) {
        // This would use SimHashService, but we keep value objects pure
        throw new UnsupportedOperationException("Use SimHashService to calculate from content");
    }

    @Override
    public String toString() {
        return String.format("%016X", value);
    }
}
