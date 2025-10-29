package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.config.KafkaTopics;
import com.chibao.edu.search_engine.dto.request.CrawlRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

/**
 * Senior Level: Sitemap.xml Parser with intelligent URL extraction
 *
 * Features:
 * - Parses sitemap.xml and sitemap index files
 * - Handles gzipped sitemaps (.xml.gz)
 * - Extracts URL priority and change frequency
 * - Supports nested sitemap indices
 * - Async processing to avoid blocking
 * - Automatic dispatch to crawl queue
 * - Respects lastmod dates for incremental crawling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SitemapParserService {

    private final KafkaTemplate<String, CrawlRequest> crawlRequestKafkaTemplate;
    private final RobotsTxtService robotsTxtService;

    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_SITEMAP_SIZE_MB = 50;
    private static final String USER_AGENT = "SearchEngineBot/1.0";

    @Data
    public static class SitemapUrl {
        private String loc;
        private LocalDateTime lastmod;
        private String changefreq;
        private Double priority;

        /**
         * Calculate crawl priority (1-10) based on sitemap metadata
         */
        public int calculateCrawlPriority() {
            int basePriority = 5;

            // Adjust based on sitemap priority
            if (priority != null) {
                basePriority = (int) Math.ceil(priority * 10);
            }

            // Boost recent changes
            if (lastmod != null) {
                long daysSinceUpdate = java.time.temporal.ChronoUnit.DAYS
                        .between(lastmod, LocalDateTime.now());

                if (daysSinceUpdate < 7) {
                    basePriority += 2; // Recent changes
                } else if (daysSinceUpdate < 30) {
                    basePriority += 1;
                } else if (daysSinceUpdate > 365) {
                    basePriority -= 1; // Old content
                }
            }

            // Adjust based on change frequency
            if (changefreq != null) {
                switch (changefreq.toLowerCase()) {
                    case "always":
                    case "hourly":
                        basePriority += 2;
                        break;
                    case "daily":
                        basePriority += 1;
                        break;
                    case "weekly":
                        // No change
                        break;
                    case "monthly":
                        basePriority -= 1;
                        break;
                    case "yearly":
                    case "never":
                        basePriority -= 2;
                        break;
                }
            }

            // Ensure priority is in valid range
            return Math.max(1, Math.min(10, basePriority));
        }
    }

    /**
     * Process sitemap URLs from domain
     */
    @Async
    public CompletableFuture<Integer> processSitemapsForDomain(String domain) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Processing sitemaps for domain: {}", domain);

                // Get sitemap URLs from robots.txt
                List<String> sitemapUrls = robotsTxtService.getSitemaps(domain);

                if (sitemapUrls.isEmpty()) {
                    // Try default sitemap location
                    sitemapUrls = List.of(
                            "https://" + domain + "/sitemap.xml",
                            "https://" + domain + "/sitemap_index.xml"
                    );
                    log.debug("No sitemaps in robots.txt, trying default locations");
                }

                int totalUrlsProcessed = 0;
                for (String sitemapUrl : sitemapUrls) {
                    int processed = processSitemap(sitemapUrl, domain, 0).join();
                    totalUrlsProcessed += processed;
                }

                log.info("Processed {} URLs from sitemaps for domain {}", totalUrlsProcessed, domain);
                return totalUrlsProcessed;

            } catch (Exception e) {
                log.error("Failed to process sitemaps for domain {}", domain, e);
                return 0;
            }
        });
    }

    /**
     * Parse and process a single sitemap (handles both sitemap and sitemap index)
     */
    @Async
    public CompletableFuture<Integer> processSitemap(String sitemapUrl, String domain, int depth) {
        return CompletableFuture.supplyAsync(() -> {
            // Prevent infinite recursion
            if (depth > 3) {
                log.warn("Maximum sitemap depth reached for {}", sitemapUrl);
                return 0;
            }

            try {
                log.info("Fetching sitemap: {}", sitemapUrl);

                Document doc = fetchAndParseSitemap(sitemapUrl);
                if (doc == null) {
                    return 0;
                }

                // Check if it's a sitemap index
                NodeList sitemapNodes = doc.getElementsByTagName("sitemap");
                if (sitemapNodes.getLength() > 0) {
                    return processSitemapIndex(doc, domain, depth);
                }

                // It's a regular sitemap with URLs
                return processSitemapUrls(doc, domain);

            } catch (Exception e) {
                log.error("Failed to process sitemap {}", sitemapUrl, e);
                return 0;
            }
        });
    }

    /**
     * Fetch and parse sitemap XML document
     */
    private Document fetchAndParseSitemap(String sitemapUrl) {
        InputStream inputStream = null;

        try {
            URL url = new URL(sitemapUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                log.debug("Sitemap not found or error: {} (status {})", sitemapUrl, responseCode);
                return null;
            }

            // Check content length
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_SITEMAP_SIZE_MB * 1024 * 1024) {
                log.warn("Sitemap too large: {} MB", contentLength / (1024 * 1024));
                return null;
            }

            // Get input stream
            inputStream = connection.getInputStream();

            // Handle gzipped sitemaps
            if (sitemapUrl.endsWith(".gz")) {
                inputStream = new GZIPInputStream(inputStream);
            }

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(inputStream);

        } catch (Exception e) {
            log.error("Failed to fetch sitemap: {}", sitemapUrl, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Process sitemap index (contains links to other sitemaps)
     */
    private int processSitemapIndex(Document doc, String domain, int depth) {
        int totalProcessed = 0;

        NodeList sitemapNodes = doc.getElementsByTagName("sitemap");
        log.info("Found sitemap index with {} sitemaps", sitemapNodes.getLength());

        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < sitemapNodes.getLength(); i++) {
            Node sitemapNode = sitemapNodes.item(i);
            if (sitemapNode.getNodeType() == Node.ELEMENT_NODE) {
                Element sitemapElement = (Element) sitemapNode;

                String loc = getElementText(sitemapElement, "loc");
                if (loc != null && !loc.isEmpty()) {
                    // Process nested sitemap recursively
                    CompletableFuture<Integer> future = processSitemap(loc, domain, depth + 1);
                    futures.add(future);
                }
            }
        }

        // Wait for all nested sitemaps to complete
        for (CompletableFuture<Integer> future : futures) {
            totalProcessed += future.join();
        }

        return totalProcessed;
    }

    /**
     * Process URLs from sitemap
     */
    private int processSitemapUrls(Document doc, String domain) {
        NodeList urlNodes = doc.getElementsByTagName("url");
        log.info("Found {} URLs in sitemap", urlNodes.getLength());

        List<SitemapUrl> sitemapUrls = new ArrayList<>();

        for (int i = 0; i < urlNodes.getLength(); i++) {
            Node urlNode = urlNodes.item(i);
            if (urlNode.getNodeType() == Node.ELEMENT_NODE) {
                Element urlElement = (Element) urlNode;

                SitemapUrl sitemapUrl = new SitemapUrl();
                sitemapUrl.setLoc(getElementText(urlElement, "loc"));

                // Parse lastmod
                String lastmodStr = getElementText(urlElement, "lastmod");
                if (lastmodStr != null) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(lastmodStr,
                                DateTimeFormatter.ISO_DATE_TIME);
                        sitemapUrl.setLastmod(zdt.toLocalDateTime());
                    } catch (Exception e) {
                        // Try other date formats
                        try {
                            sitemapUrl.setLastmod(LocalDateTime.parse(lastmodStr));
                        } catch (Exception ignored) {}
                    }
                }

                // Parse priority
                String priorityStr = getElementText(urlElement, "priority");
                if (priorityStr != null) {
                    try {
                        sitemapUrl.setPriority(Double.parseDouble(priorityStr));
                    } catch (NumberFormatException ignored) {}
                }

                // Parse changefreq
                sitemapUrl.setChangefreq(getElementText(urlElement, "changefreq"));

                if (sitemapUrl.getLoc() != null) {
                    sitemapUrls.add(sitemapUrl);
                }
            }
        }

        // Dispatch URLs to crawl queue
        return dispatchUrlsToCrawl(sitemapUrls, domain);
    }

    /**
     * Dispatch sitemap URLs to Kafka for crawling
     */
    private int dispatchUrlsToCrawl(List<SitemapUrl> sitemapUrls, String domain) {
        int dispatched = 0;

        for (SitemapUrl sitemapUrl : sitemapUrls) {
            try {
                CrawlRequest request = CrawlRequest.builder()
                        .url(sitemapUrl.getLoc())
                        .domain(domain)
                        .depth(0) // Sitemap URLs are treated as seeds
                        .priority(sitemapUrl.calculateCrawlPriority())
                        .timestamp(LocalDateTime.now())
                        .build();

                crawlRequestKafkaTemplate.send(
                        KafkaTopics.CRAWL_REQUESTS,
                        domain,
                        request
                );

                dispatched++;

                // Batch logging
                if (dispatched % 100 == 0) {
                    log.debug("Dispatched {} sitemap URLs for domain {}", dispatched, domain);
                }

            } catch (Exception e) {
                log.error("Failed to dispatch sitemap URL: {}", sitemapUrl.getLoc(), e);
            }
        }

        log.info("Dispatched {} URLs from sitemap for domain {}", dispatched, domain);
        return dispatched;
    }

    /**
     * Helper method to extract text from XML element
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null) {
                String text = node.getTextContent();
                return text != null ? text.trim() : null;
            }
        }
        return null;
    }

    /**
     * Analyze sitemap structure (for debugging/monitoring)
     */
    public CompletableFuture<Map<String, Object>> analyzeSitemap(String sitemapUrl) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> analysis = new HashMap<>();

            try {
                Document doc = fetchAndParseSitemap(sitemapUrl);
                if (doc == null) {
                    analysis.put("error", "Failed to fetch sitemap");
                    return analysis;
                }

                // Check type
                NodeList sitemapNodes = doc.getElementsByTagName("sitemap");
                boolean isIndex = sitemapNodes.getLength() > 0;

                analysis.put("type", isIndex ? "sitemap_index" : "sitemap");

                if (isIndex) {
                    analysis.put("child_sitemaps", sitemapNodes.getLength());
                } else {
                    NodeList urlNodes = doc.getElementsByTagName("url");
                    analysis.put("url_count", urlNodes.getLength());

                    // Sample URLs
                    List<String> sampleUrls = new ArrayList<>();
                    for (int i = 0; i < Math.min(5, urlNodes.getLength()); i++) {
                        Element urlElement = (Element) urlNodes.item(i);
                        String loc = getElementText(urlElement, "loc");
                        if (loc != null) {
                            sampleUrls.add(loc);
                        }
                    }
                    analysis.put("sample_urls", sampleUrls);
                }

            } catch (Exception e) {
                analysis.put("error", e.getMessage());
            }

            return analysis;
        });
    }
}