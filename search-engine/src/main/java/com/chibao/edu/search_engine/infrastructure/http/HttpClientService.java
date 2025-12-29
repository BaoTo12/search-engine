package com.chibao.edu.search_engine.infrastructure.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP Client service for fetching web pages.
 * Infrastructure service wrapping Java's HttpClient.
 */
@Service
@Slf4j
public class HttpClientService {

    private final HttpClient httpClient;
    private static final String USER_AGENT = "SearchEngineBot/1.0 (+https://example.com/bot)";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    public HttpClientService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL) // Follow redirects
                .build();
    }

    /**
     * Fetch a URL and return the HTTP response.
     */
    public HttpFetchResult fetch(String url) {
        try {
            log.debug("Fetching URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(READ_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .GET()
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Fetched {} - Status: {} - Duration: {}ms", url, response.statusCode(), duration);

            return HttpFetchResult.builder()
                    .url(url)
                    .statusCode(response.statusCode())
                    .contentType(response.headers().firstValue("Content-Type").orElse("text/html"))
                    .content(response.body())
                    .contentLength(response.body().length())
                    .responseTimeMs((int) duration)
                    .success(response.statusCode() >= 200 && response.statusCode() < 300)
                    .build();

        } catch (IOException e) {
            log.error("IOException while fetching {}: {}", url, e.getMessage());
            return HttpFetchResult.builder()
                    .url(url)
                    .statusCode(0)
                    .success(false)
                    .errorMessage("IOException: " + e.getMessage())
                    .build();
        } catch (InterruptedException e) {
            log.error("Interrupted while fetching {}: {}", url, e.getMessage());
            Thread.currentThread().interrupt();
            return HttpFetchResult.builder()
                    .url(url)
                    .statusCode(0)
                    .success(false)
                    .errorMessage("Interrupted: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error while fetching {}: {}", url, e.getMessage());
            return HttpFetchResult.builder()
                    .url(url)
                    .statusCode(0)
                    .success(false)
                    .errorMessage("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Fetch robots.txt for a domain.
     */
    public String fetchRobotsTxt(String domain) {
        String robotsUrl = "https://" + domain + "/robots.txt";
        try {
            HttpFetchResult result = fetch(robotsUrl);
            if (result.isSuccess()) {
                return result.getContent();
            }
            // Try HTTP if HTTPS fails
            robotsUrl = "http://" + domain + "/robots.txt";
            result = fetch(robotsUrl);
            return result.isSuccess() ? result.getContent() : "";
        } catch (Exception e) {
            log.warn("Could not fetch robots.txt for {}: {}", domain, e.getMessage());
            return "";
        }
    }

    /**
     * Result object for HTTP fetch operations.
     */
    @lombok.Data
    @lombok.Builder
    public static class HttpFetchResult {
        private String url;
        private int statusCode;
        private String contentType;
        private String content;
        private int contentLength;
        private int responseTimeMs;
        private boolean success;
        private String errorMessage;
    }
}
