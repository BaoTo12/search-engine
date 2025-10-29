# Learning from the Distributed Search Engine Project

This document provides a guide to understanding and learning from this distributed search engine project. It's a great example of a modern, scalable, and event-driven system built with Java and Spring Boot.

## Project Overview

This project is a distributed web crawler and search engine. It's designed to be scalable, resilient, and performant. Here are the key features:

- **Distributed Crawling:** The crawling process is distributed across multiple workers, allowing for horizontal scaling.
- **Event-Driven Architecture:** The system is built around an event-driven architecture using Kafka as a message broker. This decouples the different components and makes the system more resilient.
- **Full-Text Search:** Elasticsearch is used as the search engine, providing powerful full-text search capabilities.
- **Advanced Search Features:** The search functionality is enhanced with features like query expansion, spelling correction, and result diversification.
- **PageRank Algorithm:** A simplified PageRank algorithm is implemented to rank the importance of web pages.
- **Politeness and Rate Limiting:** The crawler respects `robots.txt` files and implements rate limiting to be a good web citizen.
- **Fault Tolerance:** The system is designed to be fault-tolerant with features like circuit breakers and retry logic.
- **Monitoring:** The application exposes metrics for monitoring the health and performance of the system.

## Key Technologies

- **Spring Boot:** The core framework for building the application.
- **Kafka:** A distributed streaming platform used as a message broker.
- **Elasticsearch:** A distributed, RESTful search and analytics engine.
- **PostgreSQL (or H2):** A relational database for storing metadata.
- **Redis:** An in-memory data structure store, used as a cache and for distributed locking.
- **Jsoup:** A Java library for working with real-world HTML.
- **Lucene:** A high-performance, full-featured text search engine library.
- **Docker:** A platform for developing, shipping, and running applications in containers.

## How to Get Started

Here's a step-by-step guide to get you started with the project:

### 1. Understand the Architecture

The best way to start is to understand the overall architecture of the system. The `README.md` file provides a good overview of the architecture. Here's a quick summary:

- **Crawl Scheduler:** This component is responsible for managing the crawl queue and dispatching crawl requests to the crawler workers.
- **Crawler Workers:** These are the workhorses of the system. They fetch web pages, parse them, and extract relevant information.
- **Indexer:** This component receives the crawled data and indexes it into Elasticsearch.
- **Search Service:** This service provides the REST API for searching the indexed data.
- **Kafka:** Kafka acts as the backbone of the system, enabling asynchronous communication between the different components.

### 2. Explore the Code

Once you have a good understanding of the architecture, you can start exploring the code. Here are some key files and packages to look at:

- **`SearchEngineApplication.java`:** This is the main entry point of the application. It's a good place to see how the different components are wired together.
- **`pom.xml`:** This file defines the project's dependencies. It's a good place to see what libraries are being used.
- **`application.yml`:** This file contains the configuration for the application.
- **`com.chibao.edu.search_engine.service` package:** This package contains the core business logic of the application. Start by looking at the `CrawlSchedulerService`, `CrawlerWorkerService`, `IndexerService`, and `SearchService` classes.
- **`com.chibao.edu.search_engine.controller` package:** This package defines the REST APIs for the application.
- **`com.chibao.edu.search_engine.entity` package:** This package contains the JPA and Elasticsearch entities.

### 3. Run the Application

The `README.md` file provides instructions on how to run the application. You can either run it with Docker Compose or by setting up the required services (PostgreSQL, Kafka, Elasticsearch, Redis) manually.

If you want to simplify the setup, you can use the H2 in-memory database for testing purposes. The steps to do this are described in the conversation history.

### 4. Experiment and Learn

The best way to learn is by experimenting. Here are some ideas:

- **Add a new feature:** Try adding a new feature to the search engine, such as filtering by date or language.
- **Improve the ranking algorithm:** The current PageRank implementation is quite simple. You could try to improve it by taking other factors into account, such as the relevance of the content.
- **Optimize the performance:** Try to optimize the performance of the crawler or the indexer.
- **Add more monitoring:** Add more metrics to the application to get more insights into its performance.

## Conclusion

This project is a great learning resource for anyone interested in distributed systems, event-driven architecture, and search engines. By exploring the code and experimenting with it, you can gain a deep understanding of how these systems are built.
