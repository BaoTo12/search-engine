package com.chibao.edu.search_engine.domain.crawling.model.valueobject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class Url {
    private final String value;

    private Url(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        try {
            new URI(value); // Validate
            this.value = normalize(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + value, e);
        }
    }

    public static Url of(String value) {
        return new Url(value);
    }

    private String normalize(String url) {
        String normalized = url.trim().toLowerCase();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        try {
            URI uri = new URI(value);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Url))
            return false;
        Url url = (Url) o;
        return Objects.equals(value, url.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
