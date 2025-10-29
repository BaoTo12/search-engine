package com.chibao.edu.search_engine.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopics {
    public static final String CRAWL_REQUESTS = "crawl-requests";
    public static final String CRAWL_RESULTS = "crawl-results";
    public static final String INDEX_REQUESTS = "index-requests";
    public static final String LINK_DISCOVERIES = "link-discoveries";
    public static final String DEAD_LETTER_QUEUE = "crawl-dlq";
}
