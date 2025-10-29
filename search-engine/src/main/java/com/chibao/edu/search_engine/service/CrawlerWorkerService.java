package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.config.KafkaTopics;
import com.chibao.edu.search_engine.dto.request.CrawlRequest;
import com.chibao.edu.search_engine.dto.request.IndexRequest;
import com.chibao.edu.search_engine.dto.response.CrawlResult;
import com.chibao.edu.search_engine.dto.response.LinkDiscoveryMessage;
import com.chibao.edu.search_engine.entity.DomainMetadata;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class CrawlerWorkerService {

    private final KafkaTemplate<String, CrawlResult> crawlResultKafkaTemplate;
    private final KafkaTemplate<String, IndexRequest> indexRequestKafkaTemplate;
    private final KafkaTemplate<String, LinkDiscoveryMessage> linkDiscoveryKafkaTemplate;
    private final CrawlUrlRepository crawlUrlRepository;
    private final DomainMetadataRepository domainMetadataRepository;
    private final MeterRegistry meterRegistry;

    private final ExecutorService crawlExecutor = new ThreadPoolExecutor(
            20, 100, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private Counter successCounter;
    private Counter failureCounter;

    public CrawlerWorkerService(
            KafkaTemplate<String, CrawlResult> crawlResultKafkaTemplate,
            KafkaTemplate<String, IndexRequest> indexRequestKafkaTemplate,
            KafkaTemplate<String, LinkDiscoveryMessage> linkDiscoveryKafkaTemplate,
            CrawlUrlRepository crawlUrlRepository,
            DomainMetadataRepository domainMetadataRepository,
            MeterRegistry meterRegistry) {

        this.crawlResultKafkaTemplate = crawlResultKafkaTemplate;
        this.indexRequestKafkaTemplate = indexRequestKafkaTemplate;
        this.linkDiscoveryKafkaTemplate = linkDiscoveryKafkaTemplate;
        this.crawlUrlRepository = crawlUrlRepository;
        this.domainMetadataRepository = domainMetadataRepository;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.successCounter = Counter.builder("crawler.success")
                .description("Number of successful crawls")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("crawler.failure")
                .description("Number of failed crawls")
                .register(meterRegistry);
    }

    /**
     * Listen to crawl requests from Kafka
     */
    @KafkaListener(
            topics = KafkaTopics.CRAWL_REQUESTS,
            containerFactory = "crawlRequestListenerFactory"
    )
    public void processCrawlRequest(CrawlRequest request, Acknowledgment ack) {
        log.info("Processing crawl request for: {}", request.getUrl());

        CompletableFuture.runAsync(() -> {
            try {
                crawlPage(request);
                ack.acknowledge();
            } catch (Exception e) {
                log.error("Error processing crawl request: {}", request.getUrl(), e);
                handleCrawlFailure(request, e.getMessage());
                ack.acknowledge();
            }
        }, crawlExecutor);
    }

    /**
     * Main crawling logic
     */
    private void crawlPage(CrawlRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Fetch and parse HTML
            Document doc = Jsoup.connect(request.getUrl())
                    .userAgent("SearchEngineBot/1.0 (+http://yourdomain.com/bot)")
                    .timeout(15000)
                    .followRedirects(true)
                    .maxBodySize(5 * 1024 * 1024) // 5MB limit
                    .get();

            // Extract metadata
            String title = doc.title();
            String content = extractTextContent(doc);
            List<String> outboundLinks = extractLinks(doc, request.getUrl());

            // Build crawl result
//            CrawlResult result = CrawlResult.builder()
//                    .url(request.getUrl())
//                    .domain(request.getDomain())
//                    .depth(request.getDepth())
//                    .title(title)
//                    .content(content)
//                    .outboundLinks(outboundLinks)
//                    .httpStatusCode(200)
//                    .success(true)
//                    .crawledAt(LocalDateTime.now())
//                    .build();

            // Send for indexing
            IndexRequest indexRequest = IndexRequest.builder()
                    .url(request.getUrl())
                    .title(title)
                    .content(content)
                    .outboundLinks(outboundLinks)
                    .domain(request.getDomain())
                    .depth(request.getDepth())
                    .crawledAt(LocalDateTime.now())
                    .build();

            indexRequestKafkaTemplate.send(KafkaTopics.INDEX_REQUESTS, request.getUrl(), indexRequest);

            // Send discovered links
            if (!outboundLinks.isEmpty()) {
                LinkDiscoveryMessage linkMsg = LinkDiscoveryMessage.builder()
                        .urls(outboundLinks)
                        .sourceDomain(request.getDomain())
                        .sourceDepth(request.getDepth())
                        .build();

                linkDiscoveryKafkaTemplate.send(KafkaTopics.LINK_DISCOVERIES, request.getDomain(), linkMsg);
            }

            // Update crawl status
            updateCrawlStatus(request.getUrl(), CrawlStatus.COMPLETED, null);

            // Update domain metadata
            updateDomainMetadata(request.getDomain(), true);

            // Metrics
            successCounter.increment();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully crawled {} in {}ms, found {} links",
                    request.getUrl(), duration, outboundLinks.size());

        } catch (IOException e) {
            log.error("Failed to crawl {}: {}", request.getUrl(), e.getMessage());
            handleCrawlFailure(request, e.getMessage());
            failureCounter.increment();
        }
    }

    /**
     * Extract clean text content from HTML
     */
    private String extractTextContent(Document doc) {
        // Remove script and style elements
        doc.select("script, style, nav, footer, header").remove();

        // Get text from body
        Element body = doc.body();
        if (body == null) {
            return "";
        }

        String text = body.text();

        // Clean whitespace
        text = text.replaceAll("\\s+", " ").trim();

        // Limit content size (100KB)
        if (text.length() > 100000) {
            text = text.substring(0, 100000);
        }

        return text;
    }

    /**
     * Extract all valid outbound links
     */
    private List<String> extractLinks(Document doc, String baseUrl) {
        List<String> links = new ArrayList<>();
        Elements linkElements = doc.select("a[href]");

        for (Element link : linkElements) {
            try {
                String href = link.absUrl("href");

                // Filter valid HTTP/HTTPS links
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    // Remove fragment
                    int fragmentIndex = href.indexOf('#');
                    if (fragmentIndex != -1) {
                        href = href.substring(0, fragmentIndex);
                    }

                    // Avoid common non-HTML resources
                    if (!href.matches(".*\\.(jpg|jpeg|png|gif|pdf|zip|exe|mp4|mp3)$")) {
                        links.add(href);
                    }
                }
            } catch (Exception e) {
                // Skip invalid links
            }
        }

        return links;
    }

    /**
     * Handle crawl failure
     */
    private void handleCrawlFailure(CrawlRequest request, String errorMessage) {
        updateCrawlStatus(request.getUrl(), CrawlStatus.FAILED, errorMessage);
        updateDomainMetadata(request.getDomain(), false);

        // Send to dead letter queue for analysis
        CrawlResult failureResult = CrawlResult.builder()
                .url(request.getUrl())
                .domain(request.getDomain())
                .depth(request.getDepth())
                .success(false)
                .errorMessage(errorMessage)
                .crawledAt(LocalDateTime.now())
                .build();

        crawlResultKafkaTemplate.send(KafkaTopics.DEAD_LETTER_QUEUE, request.getUrl(), failureResult);
    }

    /**
     * Update crawl URL status in database
     */
    private void updateCrawlStatus(String url, CrawlStatus status, String errorMessage) {
        try {
            crawlUrlRepository.findByUrlHash(url).ifPresent(crawlUrl -> {
                crawlUrl.setStatus(status);

                if (status == CrawlStatus.COMPLETED) {
                    crawlUrl.setLastSuccessfulCrawl(LocalDateTime.now());
                    crawlUrl.setFailureCount(0);
                } else if (status == CrawlStatus.FAILED) {
                    crawlUrl.setFailureCount(crawlUrl.getFailureCount() + 1);
                    crawlUrl.setErrorMessage(errorMessage);
                }

                crawlUrlRepository.save(crawlUrl);
            });
        } catch (Exception e) {
            log.error("Failed to update crawl status for {}", url, e);
        }
    }

    /**
     * Update domain metadata statistics
     */
    private void updateDomainMetadata(String domain, boolean success) {
        try {
            DomainMetadata metadata = domainMetadataRepository.findByDomain(domain)
                    .orElse(DomainMetadata.builder()
                            .domain(domain)
                            .crawlDelayMs(1000L)
                            .maxConcurrentRequests(5)
                            .isBlocked(false)
                            .totalPagesCrawled(0)
                            .totalFailures(0)
                            .build());

            metadata.setLastCrawlTime(LocalDateTime.now());

            if (success) {
                metadata.setTotalPagesCrawled(metadata.getTotalPagesCrawled() + 1);
            } else {
                metadata.setTotalFailures(metadata.getTotalFailures() + 1);

                // Block domain if too many failures
                if (metadata.getTotalFailures() > 50) {
                    metadata.setIsBlocked(true);
                    log.warn("Blocked domain {} due to excessive failures", domain);
                }
            }

            domainMetadataRepository.save(metadata);
        } catch (Exception e) {
            log.error("Failed to update domain metadata for {}", domain, e);
        }
    }
}
