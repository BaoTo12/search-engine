package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.config.KafkaTopics;
import com.chibao.edu.search_engine.dto.request.IndexRequest;
import com.chibao.edu.search_engine.entity.WebPage;
import com.chibao.edu.search_engine.repository.elasticsearch.WebPageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class IndexerService {

    private final WebPageRepository webPageRepository;
    private final MeterRegistry meterRegistry;
    private final Analyzer analyzer = new EnglishAnalyzer();

    private Counter indexedCounter;

    public IndexerService(WebPageRepository webPageRepository, MeterRegistry meterRegistry) {
        this.webPageRepository = webPageRepository;
        this.meterRegistry = meterRegistry;

        this.indexedCounter = Counter.builder("indexer.indexed")
                .description("Number of pages indexed")
                .register(meterRegistry);
    }

    /**
     * Listen to index requests from Kafka
     */
    @KafkaListener(topics = KafkaTopics.INDEX_REQUESTS, containerFactory = "indexRequestListenerFactory")
    public void processIndexRequest(IndexRequest request, Acknowledgment ack) {
        log.info("Processing index request for: {}", request.getUrl());

        try {
            indexPage(request);
            ack.acknowledge();
            indexedCounter.increment();
        } catch (Exception e) {
            log.error("Failed to index page: {}", request.getUrl(), e);
            ack.acknowledge(); // Acknowledge to avoid reprocessing
        }
    }

    /**
     * Main indexing logic
     */
    private void indexPage(IndexRequest request) throws NoSuchAlgorithmException, IOException {
        // Generate document ID from URL
        String docId = generateDocId(request.getUrl());

        // Tokenize content
        Set<String> tokens = tokenizeContent(request.getContent());

        // Generate snippet (first 200 chars)
        String snippet = generateSnippet(request.getContent());

        // Extract domain
        String domain = extractDomain(request.getUrl());

        // Check if document already exists
        Optional<WebPage> existingPage = webPageRepository.findById(docId);

        WebPage webPage;
        if (existingPage.isPresent()) {
            // Update existing document
            webPage = existingPage.get();
            webPage.setTitle(request.getTitle());
            webPage.setContent(request.getContent());
            webPage.setSnippet(snippet);
            webPage.setOutboundLinks(request.getOutboundLinks());
            webPage.setTokens(tokens);
            webPage.setLastCrawled(request.getCrawledAt());
            webPage.setLastIndexed(LocalDateTime.now());

            log.debug("Updating existing document: {}", request.getUrl());
        } else {
            // Create new document
            webPage = WebPage.builder()
                    .id(docId)
                    .url(request.getUrl())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .snippet(snippet)
                    .outboundLinks(request.getOutboundLinks())
                    .tokens(tokens)
                    .domain(domain)
                    .crawlDepth(request.getDepth())
                    .pageRank(1.0) // Initial PageRank
                    .inboundLinkCount(0)
                    .lastCrawled(request.getCrawledAt())
                    .lastIndexed(LocalDateTime.now())
                    .contentType("text/html")
                    .contentLength((long) request.getContent().length())
                    .build();

            log.debug("Creating new document: {}", request.getUrl());
        }

        // Save to Elasticsearch
        webPageRepository.save(webPage);

        log.info("Successfully indexed: {} ({} tokens)", request.getUrl(), tokens.size());
    }

    /**
     * Tokenize content using Lucene analyzer
     * Performs: lowercasing, stopword removal, stemming
     */
    private Set<String> tokenizeContent(String content) throws IOException {
        Set<String> tokens = new HashSet<>();

        if (content == null || content.isEmpty()) {
            return tokens;
        }

        // Limit content size for tokenization
        if (content.length() > 50000) {
            content = content.substring(0, 50000);
        }

        try (TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(content))) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString();

                // Only keep tokens with length 2-30
                if (term.length() >= 2 && term.length() <= 30) {
                    tokens.add(term);
                }

                // Limit total tokens to prevent memory issues
                if (tokens.size() >= 10000) {
                    break;
                }
            }

            tokenStream.end();
        }

        return tokens;
    }

    /**
     * Generate snippet for search results
     */
    private String generateSnippet(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Remove extra whitespace
        content = content.replaceAll("\\s+", " ").trim();

        // Take first 200 characters
        if (content.length() > 200) {
            content = content.substring(0, 200);

            // Try to end at word boundary
            int lastSpace = content.lastIndexOf(' ');
            if (lastSpace > 150) {
                content = content.substring(0, lastSpace);
            }

            content += "...";
        }

        return content;
    }

    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            return url.replaceFirst("https?://", "")
                    .replaceFirst("www\\.", "")
                    .split("/")[0]
                    .toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Generate unique document ID from URL using SHA-256
     */
    private String generateDocId(String url) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Update PageRank scores (batch job)
     * This should be run periodically (e.g., weekly)
     */
    public void updatePageRankScores() {
        log.info("Starting PageRank score update...");

        try {
            // Get all documents
            Iterable<WebPage> allPages = webPageRepository.findAll();

            // Build link graph
            Map<String, List<String>> linkGraph = new HashMap<>();
            Map<String, Double> pageRanks = new HashMap<>();

            for (WebPage page : allPages) {
                linkGraph.put(page.getUrl(), page.getOutboundLinks());
                pageRanks.put(page.getUrl(), 1.0);
            }

            // Run PageRank algorithm (simplified)
            int iterations = 10;
            double dampingFactor = 0.85;

            for (int i = 0; i < iterations; i++) {
                Map<String, Double> newRanks = new HashMap<>();

                for (String url : pageRanks.keySet()) {
                    double rank = (1 - dampingFactor);

                    // Add contributions from inbound links
                    for (Map.Entry<String, List<String>> entry : linkGraph.entrySet()) {
                        if (entry.getValue().contains(url)) {
                            String inboundUrl = entry.getKey();
                            int outboundCount = entry.getValue().size();
                            if (outboundCount > 0) {
                                rank += dampingFactor * (pageRanks.get(inboundUrl) / outboundCount);
                            }
                        }
                    }

                    newRanks.put(url, rank);
                }

                pageRanks = newRanks;
            }

            // Update documents with new PageRank scores
            int updated = 0;
            for (WebPage page : allPages) {
                Double newRank = pageRanks.get(page.getUrl());
                if (newRank != null) {
                    page.setPageRank(newRank);

                    // Count inbound links
                    long inboundCount = linkGraph.values().stream()
                            .filter(links -> links.contains(page.getUrl()))
                            .count();
                    page.setInboundLinkCount((int) inboundCount);

                    webPageRepository.save(page);
                    updated++;
                }
            }

            log.info("Updated PageRank scores for {} documents", updated);

        } catch (Exception e) {
            log.error("Failed to update PageRank scores", e);
        }
    }
}
