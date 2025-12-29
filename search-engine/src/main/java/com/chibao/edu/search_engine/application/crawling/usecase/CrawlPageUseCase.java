package com.chibao.edu.search_engine.application.crawling.usecase;

import com.chibao.edu.search_engine.domain.crawling.model.aggregate.CrawlJob;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlDepth;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.CrawlStatus;
import com.chibao.edu.search_engine.domain.crawling.model.valueobject.Url;
import com.chibao.edu.search_engine.domain.crawling.repository.CrawlJobRepository;
import com.chibao.edu.search_engine.domain.crawling.service.RobotsTxtParser;
import com.chibao.edu.search_engine.domain.crawling.service.UrlNormalizationService;
import com.chibao.edu.search_engine.infrastructure.http.HttpClientService;
import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.PageContentMessage;
import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.LinkDiscoveryMessage;
import com.chibao.edu.search_engine.infrastructure.parsing.HtmlParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for crawling a single page.
 * This is the core crawling logic orchestrating all steps.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlPageUseCase {

    private final HttpClientService httpClient;
    private final HtmlParserService htmlParser;
    private final CrawlJobRepository crawlJobRepository;
    private final UrlNormalizationService urlNormalizationService;
    private final RobotsTxtParser robotsTxtParser;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void execute(String crawlJobId) {
        log.info("Executing crawl for job: {}", crawlJobId);

        try {
            // 1. Find crawl job
            CrawlJob crawlJob = crawlJobRepository.findById(crawlJobId)
                    .orElseThrow(() -> new IllegalArgumentException("Crawl job not found: " + crawlJobId));

            // 2. Mark as in progress
            crawlJob.markAsInProgress();
            crawlJobRepository.save(crawlJob);

            // 3. Fetch robots.txt and check if allowed
            String domain = urlNormalizationService.extractDomain(crawlJob.getUrl().getValue());
            String robotsTxtContent = httpClient.fetchRobotsTxt(domain);
            RobotsTxtParser.RobotsTxtRules rules = robotsTxtParser.parse(robotsTxtContent, "SearchEngineBot");

            String path = extractPath(crawlJob.getUrl().getValue());
            if (!robotsTxtParser.isAllowed(path, rules)) {
                log.warn("URL blocked by robots.txt: {}", crawlJob.getUrl().getValue());
                crawlJob.markAsFailed("Blocked by robots.txt");
                crawlJobRepository.save(crawlJob);
                return;
            }

            // 4. Fetch the page
            HttpClientService.HttpFetchResult fetchResult = httpClient.fetch(crawlJob.getUrl().getValue());

            if (!fetchResult.isSuccess()) {
                log.error("Failed to fetch {}: {}", crawlJob.getUrl().getValue(), fetchResult.getErrorMessage());
                crawlJob.markAsFailed(fetchResult.getErrorMessage());
                crawlJobRepository.save(crawlJob);
                return;
            }

            // 5. Parse HTML
            HtmlParserService.ParsedPage parsedPage = htmlParser.parse(
                    crawlJob.getUrl().getValue(),
                    fetchResult.getContent());

            if (!parsedPage.isSuccess()) {
                log.error("Failed to parse {}: {}", crawlJob.getUrl().getValue(), parsedPage.getErrorMessage());
                crawlJob.markAsFailed(parsedPage.getErrorMessage());
                crawlJobRepository.save(crawlJob);
                return;
            }

            // 6. Publish page content to Kafka for indexing
            publishPageContent(crawlJob, fetchResult, parsedPage);

            // 7. Extract and publish links
            publishDiscoveredLinks(crawlJob, parsedPage);

            // 8. Mark as completed
            crawlJob.markAsCompleted();
            crawlJobRepository.save(crawlJob);

            log.info("Successfully crawled: {}", crawlJob.getUrl().getValue());

        } catch (Exception e) {
            log.error("Error crawling job {}: {}", crawlJobId, e.getMessage(), e);
        }
    }

    private void publishPageContent(CrawlJob crawlJob,
            HttpClientService.HttpFetchResult fetchResult,
            HtmlParserService.ParsedPage parsedPage) {
        try {
            PageContentMessage message = PageContentMessage.builder()
                    .url(crawlJob.getUrl().getValue())
                    .title(parsedPage.getTitle())
                    .metaDescription(parsedPage.getMetaDescription())
                    .content(parsedPage.getTextContent())
                    .htmlContent(fetchResult.getContent())
                    .outboundLinks(parsedPage.getLinks().stream()
                            .map(HtmlParserService.Link::getUrl)
                            .collect(Collectors.toList()))
                    .statusCode(fetchResult.getStatusCode())
                    .contentType(fetchResult.getContentType())
                    .language(parsedPage.getLanguage())
                    .crawledAt(LocalDateTime.now())
                    .contentSizeBytes((long) fetchResult.getContentLength())
                    .contentHash(calculateHash(parsedPage.getTextContent()))
                    .build();

            kafkaTemplate.send("pages", crawlJob.getUrl().getValue(), message);
            log.debug("Published page content to Kafka: {}", crawlJob.getUrl().getValue());

        } catch (Exception e) {
            log.error("Error publishing page content: {}", e.getMessage());
        }
    }

    private void publishDiscoveredLinks(CrawlJob crawlJob, HtmlParserService.ParsedPage parsedPage) {
        try {
            List<LinkDiscoveryMessage> messages = parsedPage.getLinks().stream()
                    .map(link -> LinkDiscoveryMessage.builder()
                            .url(link.getUrl())
                            .sourceUrl(crawlJob.getUrl().getValue())
                            .domain(extractDomain(link.getUrl()))
                            .anchorText(link.getAnchorText())
                            .depth(crawlJob.getDepth().getValue() + 1)
                            .discoveredAt(LocalDateTime.now())
                            .isMainContent(true) // TODO: Implement smart detection
                            .build())
                    .collect(Collectors.toList());

            for (LinkDiscoveryMessage message : messages) {
                kafkaTemplate.send("new-links", message.getUrl(), message);
            }

            log.debug("Published {} links to Kafka from {}", messages.size(), crawlJob.getUrl().getValue());

        } catch (Exception e) {
            log.error("Error publishing discovered links: {}", e.getMessage());
        }
    }

    private String extractPath(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getPath();
        } catch (Exception e) {
            return "/";
        }
    }

    private String extractDomain(String url) {
        try {
            return urlNormalizationService.extractDomain(url);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
