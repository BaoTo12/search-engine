package com.chibao.edu.search_engine.domain.crawling.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service for URL normalization.
 * Ensures URLs are consistent for deduplication and storage.
 * 
 * This is a DOMAIN SERVICE - no framework dependencies!
 */
public class UrlNormalizationService {

    private static final String[] TRACKING_PARAMS = {
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid", "_ga", "mc_cid", "mc_eid"
    };

    /**
     * Normalizes a URL to a canonical form.
     * 
     * Normalization rules:
     * 1. Convert scheme and host to lowercase
     * 2. Remove default ports (80 for http, 443 for https)
     * 3. Sort query parameters alphabetically
     * 4. Remove tracking parameters
     * 5. Remove fragment identifiers (#)
     * 6. Resolve relative paths (../, ./)
     * 7. Add trailing slash to directory URLs
     * 
     * @param url The URL to normalize
     * @return Normalized URL
     * @throws IllegalArgumentException if URL is invalid
     */
    public String normalize(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            // Parse URL
            URI uri = new URI(url.trim());

            // Extract components
            String scheme = normalizeScheme(uri.getScheme());
            String host = normalizeHost(uri.getHost());
            int port = normalizePort(uri.getPort(), scheme);
            String path = normalizePath(uri.getPath());
            String query = normalizeQuery(uri.getQuery());

            // Rebuild URL
            StringBuilder normalized = new StringBuilder();
            normalized.append(scheme).append("://").append(host);

            // Only add port if it's not the default
            if (port != -1) {
                normalized.append(":").append(port);
            }

            normalized.append(path);

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            return normalized.toString();

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    /**
     * Extracts the domain from a URL.
     */
    public String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    /**
     * Checks if a URL is valid and crawlable.
     */
    public boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);

            // Must have scheme and host
            if (uri.getScheme() == null || uri.getHost() == null) {
                return false;
            }

            // Only HTTP/HTTPS
            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return false;
            }

            // Host must not be empty
            if (uri.getHost().isEmpty()) {
                return false;
            }

            return true;

        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Resolves a relative URL against a base URL.
     */
    public String resolve(String baseUrl, String relativeUrl) {
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(relativeUrl);
            return normalize(resolved.toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    // ============ Private Helper Methods ============

    private String normalizeScheme(String scheme) {
        if (scheme == null) {
            return "http"; // Default to HTTP
        }
        return scheme.toLowerCase();
    }

    private String normalizeHost(String host) {
        if (host == null) {
            throw new IllegalArgumentException("URL must have a host");
        }
        return host.toLowerCase();
    }

    private int normalizePort(int port, String scheme) {
        // Remove default ports
        if (port == 80 && scheme.equals("http")) {
            return -1;
        }
        if (port == 443 && scheme.equals("https")) {
            return -1;
        }
        return port;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Remove ./ and ../
        String[] parts = path.split("/");
        java.util.List<String> normalized = new java.util.ArrayList<>();

        for (String part : parts) {
            if (part.equals(".") || part.isEmpty()) {
                continue; // Skip current directory refs
            } else if (part.equals("..")) {
                if (!normalized.isEmpty()) {
                    normalized.remove(normalized.size() - 1); // Go up one level
                }
            } else {
                normalized.add(part);
            }
        }

        String result = "/" + String.join("/", normalized);

        // Preserve trailing slash if original had it
        if (path.endsWith("/") && !result.endsWith("/")) {
            result += "/";
        }

        return result;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        // Parse query parameters
        Map<String, String> params = new LinkedHashMap<>();
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";

            // Skip tracking parameters
            if (!isTrackingParameter(key)) {
                params.put(key, value);
            }
        }

        if (params.isEmpty()) {
            return null;
        }

        // Sort parameters and rebuild query
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + (e.getValue().isEmpty() ? "" : "=" + e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private boolean isTrackingParameter(String paramName) {
        return Arrays.stream(TRACKING_PARAMS)
                .anyMatch(tp -> tp.equalsIgnoreCase(paramName));
    }
}
