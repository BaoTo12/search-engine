package com.chibao.edu.search_engine.domain.crawling.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object representing a URL.
 * Immutable and self-validating.
 */
@Getter
@EqualsAndHashCode
public final class Url {

    private final String value;

    private Url(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid URL format: " + value);
        }
        this.value = value;
    }

    public static Url of(String value) {
        return new Url(value);
    }

    private boolean isValid(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
