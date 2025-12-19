package com.chibao.edu.search_engine.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * URL Normalization Service for canonical URL generation.
 * 
 * Normalization rules (following RFC 3986 and web best practices):
 * 1. Convert scheme and host to lowercase
 * 2. Remove default ports (80 for HTTP, 443 for HTTPS)
 * 3. Remove fragment identifiers (#section)
 * 4. Sort query parameters alphabetically
 * 5. Remove tracking parameters (utm_*, fbclid, etc.)
 * 6. Resolve relative paths (/../, /./)
 * 7. Add trailing slash to directories
 * 8. URL decode and re-encode consistently
 * 
 * This ensures that duplicate URLs are recognized as the same.
 */
@Service
public class URLNormalizationService {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid", "mc_cid", "mc_eid",
            "_ga", "_gid", "ref", "referrer");

    /**
     * Normalize a URL to its canonical form.
     *
     * @param url The URL to normalize
     * @return Normalized URL, or null if invalid
     */
    public String normalize(String url) {
        if (url == null || url.isBlank()) {
            return null;
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

            if (port != -1) {
                normalized.append(":").append(port);
            }

            normalized.append(path);

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            return normalized.toString();

        } catch (Exception e) {
            // Invalid URL
            return null;
        }
    }

    /**
     * Normalize scheme (protocol) to lowercase.
     */
    private String normalizeScheme(String scheme) {
        if (scheme == null) {
            return "http"; // Default to HTTP
        }
        return scheme.toLowerCase();
    }

    /**
     * Normalize host to lowercase.
     */
    private String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        return host.toLowerCase();
    }

    /**
     * Normalize port (remove default ports).
     */
    private int normalizePort(int port, String scheme) {
        if (port == -1) {
            return -1; // No port specified
        }

        // Remove default ports
        if ((scheme.equals("http") && port == 80) ||
                (scheme.equals("https") && port == 443)) {
            return -1;
        }

        return port;
    }

    /**
     * Normalize path (resolve relative paths, add trailing slash).
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Decode path
        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);

        // Resolve relative paths
        String resolved = resolveRelativePath(decoded);

        // Add trailing slash for directories (heuristic: no file extension)
        if (!resolved.endsWith("/") && !resolved.contains(".")) {
            resolved += "/";
        }

        return resolved;
    }

    /**
     * Resolve relative path components (../, ./).
     */
    private String resolveRelativePath(String path) {
        String[] parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();

        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue; // Skip empty and current directory
            } else if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.pollLast(); // Go up one directory
                }
            } else {
                stack.offerLast(part);
            }
        }

        if (stack.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", stack);
    }

    /**
     * Normalize query string (remove tracking params, sort alphabetically).
     */
    private String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        // Parse query parameters
        Map<String, String> params = parseQueryParams(query);

        // Remove tracking parameters
        params.keySet().removeAll(TRACKING_PARAMS);

        if (params.isEmpty()) {
            return null;
        }

        // Sort parameters alphabetically and rebuild query string
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * Parse query string into key-value pairs.
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new LinkedHashMap<>();

        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }

        return params;
    }

    /**
     * Extract domain from URL.
     */
    public String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if two URLs are equivalent after normalization.
     */
    public boolean areEquivalent(String url1, String url2) {
        String norm1 = normalize(url1);
        String norm2 = normalize(url2);
        return norm1 != null && norm1.equals(norm2);
    }
}
