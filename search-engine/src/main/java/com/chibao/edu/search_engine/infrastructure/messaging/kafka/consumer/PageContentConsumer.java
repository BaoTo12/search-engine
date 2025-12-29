package com.chibao.edu.search_engine.infrastructure.messaging.kafka.consumer;

import com.chibao.edu.search_engine.infrastructure.messaging.kafka.model.PageContentMessage;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document.WebPageEsDocument;
import com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository.WebPageEsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka consumer for pages topic.
 * Indexes crawled pages in Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PageContentConsumer {

    private final WebPageEsRepository webPageRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "pages", groupId = "indexer-workers", concurrency = "4")
    public void consume(String message, Acknowledgment ack) {
        try {
            // Deserialize message
            PageContentMessage pageMessage = objectMapper.readValue(message, PageContentMessage.class);

            log.info("Indexing page: {}", pageMessage.getUrl());

            // Process and tokenize content
            List<String> tokens = tokenize(pageMessage.getContent());

            // Create Elasticsearch document
            WebPageEsDocument document = WebPageEsDocument.builder()
                    .url(pageMessage.getUrl())
                    .title(pageMessage.getTitle())
                    .metaDescription(pageMessage.getMetaDescription())
                    .content(pageMessage.getContent())
                    .tokens(tokens)
                    .outboundLinks(pageMessage.getOutboundLinks())
                    .language(pageMessage.getLanguage())
                    .contentHash(pageMessage.getContentHash())
                    .crawledAt(pageMessage.getCrawledAt())
                    .contentSizeBytes(pageMessage.getContentSizeBytes())
                    .build();

            // Save to Elasticsearch
            webPageRepository.save(document);

            log.info("Successfully indexed: {}", pageMessage.getUrl());

            // Acknowledge message
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error indexing page: {}", e.getMessage(), e);
        }
    }

    /**
     * Simple tokenization - split by whitespace and convert to lowercase.
     * TODO: Implement proper tokenization with stop word removal and stemming.
     */
    private List<String> tokenize(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(content.toLowerCase().split("\\s+"))
                .filter(token -> token.length() > 2) // Filter out very short tokens
                .filter(token -> !token.matches("\\d+")) // Filter out pure numbers
                .distinct()
                .limit(1000) // Limit tokens to prevent memory issues
                .collect(Collectors.toList());
    }
}
