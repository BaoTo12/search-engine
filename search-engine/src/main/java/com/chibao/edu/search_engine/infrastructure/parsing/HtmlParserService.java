package com.chibao.edu.search_engine.infrastructure.parsing;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTML Parser service using JSoup.
 * Extracts content, metadata, and links from HTML pages.
 */
@Service
@Slf4j
public class HtmlParserService {

    /**
     * Parse HTML content and extract all relevant information.
     */
    public ParsedPage parse(String url, String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent, url);

            return ParsedPage.builder()
                    .url(url)
                    .title(extractTitle(doc))
                    .metaDescription(extractMetaDescription(doc))
                    .textContent(extractTextContent(doc))
                    .links(extractLinks(doc, url))
                    .language(extractLanguage(doc))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing HTML for {}: {}", url, e.getMessage());
            return ParsedPage.builder()
                    .url(url)
                    .success(false)
                    .errorMessage("Parsing error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract page title.
     */
    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("title");
        if (titleElement != null) {
            return titleElement.text().trim();
        }

        // Fallback to h1
        Element h1 = doc.selectFirst("h1");
        return h1 != null ? h1.text().trim() : "Untitled";
    }

    /**
     * Extract meta description.
     */
    private String extractMetaDescription(Document doc) {
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            return metaDesc.attr("content").trim();
        }

        // Fallback to og:description
        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null) {
            return ogDesc.attr("content").trim();
        }

        return "";
    }

    /**
     * Extract main text content.
     * Removes scripts, styles, and navigation elements.
     */
    private String extractTextContent(Document doc) {
        // Remove unwanted elements
        doc.select("script, style, nav, header, footer, aside, iframe").remove();

        // Get text from body
        Element body = doc.body();
        if (body == null) {
            return "";
        }

        String text = body.text();

        // Clean up whitespace
        text = text.replaceAll("\\s+", " ").trim();

        // Truncate if too long (to avoid memory issues)
        if (text.length() > 100000) {
            text = text.substring(0, 100000);
        }

        return text;
    }

    /**
     * Extract all links from the page.
     */
    private List<Link> extractLinks(Document doc, String baseUrl) {
        Elements linkElements = doc.select("a[href]");
        List<Link> links = new ArrayList<>();

        for (Element link : linkElements) {
            try {
                String href = link.attr("abs:href"); // Get absolute URL
                String anchorText = link.text().trim();

                // Skip empty, javascript, mailto, etc.
                if (href.isEmpty() ||
                        href.startsWith("javascript:") ||
                        href.startsWith("mailto:") ||
                        href.startsWith("tel:") ||
                        href.startsWith("#")) {
                    continue;
                }

                // Only HTTP/HTTPS
                if (!href.startsWith("http://") && !href.startsWith("https://")) {
                    continue;
                }

                links.add(Link.builder()
                        .url(href)
                        .anchorText(anchorText)
                        .isExternal(isExternalLink(href, baseUrl))
                        .build());

            } catch (Exception e) {
                log.debug("Error extracting link: {}", e.getMessage());
            }
        }

        return links.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Extract language from HTML lang attribute.
     */
    private String extractLanguage(Document doc) {
        Element html = doc.selectFirst("html");
        if (html != null) {
            String lang = html.attr("lang");
            if (!lang.isEmpty()) {
                return lang;
            }
        }

        // Fallback to content-language meta tag
        Element metaLang = doc.selectFirst("meta[http-equiv=content-language]");
        if (metaLang != null) {
            return metaLang.attr("content");
        }

        return "en"; // Default to English
    }

    /**
     * Check if a link is external (different domain).
     */
    private boolean isExternal(String linkUrl, String baseUrl) {
        try {
            String linkDomain = new java.net.URI(linkUrl).getHost();
            String baseDomain = new java.net.URI(baseUrl).getHost();
            return !linkDomain.equals(baseDomain);
        } catch (Exception e) {
            return true; // Assume external if can't determine
        }
    }

    private boolean isExternalLink(String href, String baseUrl) {
        return isExternal(href, baseUrl);
    }

    /**
     * Result object for parsed page.
     */
    @Data
    @Builder
    public static class ParsedPage {
        private String url;
        private String title;
        private String metaDescription;
        private String textContent;
        private List<Link> links;
        private String language;
        private boolean success;
        private String errorMessage;
    }

    /**
     * Represents a link found on a page.
     */
    @Data
    @Builder
    public static class Link {
        private String url;
        private String anchorText;
        private boolean isExternal;
    }
}
