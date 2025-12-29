package com.chibao.edu.search_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application.
 * Clean Architecture + DDD implementation.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository")
@EnableElasticsearchRepositories(basePackages = "com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.repository")
public class SearchEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineApplication.class, args);
    }
}
