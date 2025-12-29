package com.chibao.edu.search_engine.domain.crawling.service;

import lombok.Data;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Domain service for parsing and interpreting robots.txt files.
 * RFC 9309 compliant implementation.
 * 
 * This is a DOMAIN SERVICE - no framework dependencies!
 */
public class RobotsTxtParser {

    private static final String DEFAULT_USER_AGENT = "*";
    private static final int DEFAULT_CRAWL_DELAY = 1; // seconds

    /**
     * Parses robots.txt content and returns rules.
     */
    public RobotsTxtRules parse(String content, String userAgent) {
        if (content == null || content.isBlank()) {
            return RobotsTxtRules.allowAll();
        }

        RobotsTxtRules.Builder builder = RobotsTxtRules.builder();
        List<String> sitemaps = new ArrayList<>();

        String[] lines = content.split("\\r?\\n");
        String currentUserAgent = null;
        boolean isRelevantSection = false;

        for (String line : lines) {
            line = line.trim();

            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Remove inline comments
            int commentIndex = line.indexOf('#');
            if (commentIndex > 0) {
                line = line.substring(0, commentIndex).trim();
            }

            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String directive = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            switch (directive) {
                case "user-agent":
                    currentUserAgent = value;
                    isRelevantSection = matchesUserAgent(userAgent, value);
                    break;

                case "disallow":
                    if (isRelevantSection) {
                        if (value.isEmpty()) {
                            // Empty disallow means allow all
                            continue;
                        }
                        builder.addDisallowedPath(value);
                    }
                    break;

                case "allow":
                    if (isRelevantSection) {
                        builder.addAllowedPath(value);
                    }
                    break;

                case "crawl-delay":
                    if (isRelevantSection) {
                        try {
                            int delay = Integer.parseInt(value);
                            builder.crawlDelay(delay);
                        } catch (NumberFormatException e) {
                            // Invalid delay, use default
                        }
                    }
                    break;

                case "sitemap":
                    sitemaps.add(value);
                    break;

                default:
                    // Unknown directive, ignore
                    break;
            }
        }

        builder.sitemaps(sitemaps);
        return builder.build();
    }

    /**
     * Checks if a URL path is allowed according to robots.txt rules.
     */
    public boolean isAllowed(String path, RobotsTxtRules rules) {
        if (rules == null || rules.isAllowAll()) {
            return true;
        }

        // Check allow rules first (more specific)
        for (String allowPattern : rules.getAllowedPaths()) {
            if (matchesPattern(path, allowPattern)) {
                return true;
            }
        }

        // Then check disallow rules
        for (String disallowPattern : rules.getDisallowedPaths()) {
            if (matchesPattern(path, disallowPattern)) {
                return false;
            }
        }

        // Default: allowed
        return true;
    }

    // ============ Private Helper Methods ============

    private boolean matchesUserAgent(String requestedAgent, String ruleAgent) {
        if (ruleAgent.equals(DEFAULT_USER_AGENT)) {
            return true; // * matches all
        }
        return requestedAgent.toLowerCase().contains(ruleAgent.toLowerCase());
    }

    private boolean matchesPattern(String path, String pattern) {
        // Convert robots.txt pattern to regex
        // * matches any sequence of characters
        // $ matches end of URL

        String regex = pattern
                .replace(".", "\\.") // Escape dots
                .replace("*", ".*") // * -> .*
                .replace("$", "\\$"); // Escape $

        if (pattern.endsWith("$")) {
            regex = "^" + regex;
        } else {
            regex = "^" + regex;
        }

        return Pattern.matches(regex, path);
    }

    // ============ RobotsTxtRules Value Object ============

    @Data
    public static class RobotsTxtRules {
        private final List<String> disallowedPaths;
        private final List<String> allowedPaths;
        private final int crawlDelay;
        private final List<String> sitemaps;
        private final boolean allowAll;

        private RobotsTxtRules(List<String> disallowedPaths, List<String> allowedPaths,
                int crawlDelay, List<String> sitemaps, boolean allowAll) {
            this.disallowedPaths = Collections.unmodifiableList(disallowedPaths);
            this.allowedPaths = Collections.unmodifiableList(allowedPaths);
            this.crawlDelay = crawlDelay;
            this.sitemaps = Collections.unmodifiableList(sitemaps);
            this.allowAll = allowAll;
        }

        public static RobotsTxtRules allowAll() {
            return new RobotsTxtRules(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    DEFAULT_CRAWL_DELAY,
                    Collections.emptyList(),
                    true);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final List<String> disallowedPaths = new ArrayList<>();
            private final List<String> allowedPaths = new ArrayList<>();
            private int crawlDelay = DEFAULT_CRAWL_DELAY;
            private final List<String> sitemaps = new ArrayList<>();

            public Builder addDisallowedPath(String path) {
                disallowedPaths.add(path);
                return this;
            }

            public Builder addAllowedPath(String path) {
                allowedPaths.add(path);
                return this;
            }

            public Builder crawlDelay(int delay) {
                this.crawlDelay = delay;
                return this;
            }

            public Builder sitemaps(List<String> sitemaps) {
                this.sitemaps.addAll(sitemaps);
                return this;
            }

            public RobotsTxtRules build() {
                return new RobotsTxtRules(
                        disallowedPaths,
                        allowedPaths,
                        crawlDelay,
                        sitemaps,
                        false);
            }
        }
    }
}
