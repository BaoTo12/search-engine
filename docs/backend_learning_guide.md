# 🎓 Backend Learning Guide - Clean Architecture + DDD

> **Your Complete Roadmap to Understanding This Production-Grade Search Engine**

This backend implements **Clean Architecture** with **Domain-Driven Design (DDD)** tactical patterns, ensuring framework independence, testability, and maintainability.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Clean Architecture Layers](#clean-architecture-layers)
5. [Domain-Driven Design Patterns](#domain-driven-design-patterns)
6. [Dependency Flow](#dependency-flow)
7. [Reading Roadmap](#reading-roadmap)
8. [Key Concepts](#key-concepts)
9. [Benefits of This Architecture](#benefits-of-this-architecture)

---

## 🏗️ Architecture Overview

This is a **Clean Architecture + DDD** implementation following the **Hexagonal Architecture** (Ports & Adapters) pattern.

### Core Principles

1. **Dependency Rule**: Dependencies point INWARD only
   ```
   Domain ← Application ← Infrastructure
   Domain ← Application ← Presentation
   ```

2. **Framework Independence**: Domain layer has ZERO external dependencies
3. **Testability**: Business logic can be tested without frameworks
4. **Flexibility**: Swap implementations (Elasticsearch → Solr) without touching domain

### High-Level Architecture

```
┌─────────────────────────────────────┐
│    Presentation Layer               │  REST Controllers
│    (presentation/rest/)             │
└─────────────────────────────────────┘
              ↓ uses
┌─────────────────────────────────────┐
│    Application Layer                │  Use Cases, DTOs, Ports
│    (application/)                   │
└─────────────────────────────────────┘
              ↓ uses
┌─────────────────────────────────────┐
│    Domain Layer (CORE)              │  Aggregates, Value Objects,
│    (domain/)                        │  Domain Events, Repository
│    Framework-FREE!                  │  Interfaces
└─────────────────────────────────────┘
              ↑ implements
┌─────────────────────────────────────┐
│    Infrastructure Layer             │  JPA, Elasticsearch, Redis,
│    (infrastructure/)                │  Kafka Adapters
└─────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

- **Backend Framework**: Spring Boot 3.5.7 (Java 21)
- **Search Engine**: Elasticsearch 8.x
- **Database**: PostgreSQL (with Flyway migrations)
- **Cache**: Redis
- **Message Broker**: Apache Kafka
- **Text Processing**: Apache Lucene
- **Metrics**: Micrometer, Prometheus
- **API Docs**: SpringDoc OpenAPI

---

## 📁 Project Structure

```
src/main/java/com/chibao/edu/search_engine/
│
├── 📄 SearchEngineApplication.java          # Main Spring Boot entry point
│
├── 🟦 domain/                               # FRAMEWORK-FREE Business Logic
│   ├── search/                              # Search Bounded Context
│   │   ├── model/
│   │   │   ├── valueobject/
│   │   │   │   ├── SearchQuery.java         # Immutable, self-validating
│   │   │   │   ├── SearchScore.java
│   │   │   │   └── Pagination.java
│   │   │   └── entity/
│   │   │       └── SearchResultEntity.java
│   │   └── repository/
│   │       └── SearchRepository.java        # Interface (Port)
│   │
│   ├── crawling/                            # Crawling Bounded Context
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   │   └── CrawlJob.java            # Aggregate Root
│   │   │   └── valueobject/
│   │   │       ├── Url.java
│   │   │       ├── CrawlDepth.java
│   │   │       └── CrawlStatus.java
│   │   ├── event/
│   │   │   ├── CrawlCompletedEvent.java
│   │   │   └── CrawlFailedEvent.java
│   │   ├── repository/
│   │   │   └── CrawlJobRepository.java      # Interface
│   │   └── strategy/
│   │       └── PrioritizationStrategy.java
│   │
│   ├── indexing/                            # Indexing Bounded Context
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   │   └── WebDocument.java
│   │   │   └── valueobject/
│   │   │       └── ContentHash.java         # SimHash fingerprint
│   │   ├── repository/
│   │   │   └── WebDocumentRepository.java
│   │   └── service/
│   │       └── DeduplicationService.java
│   │
│   └── ranking/                             # Ranking Bounded Context
│       ├── model/
│       │   ├── aggregate/
│       │   │   └── PageGraph.java
│       │   └── valueobject/
│       │       └── PageRankScore.java
│       ├── repository/
│       │   └── PageGraphRepository.java
│       └── service/
│           └── PageRankCalculator.java      # Domain Service
│
├── 🟩 application/                          # Use Cases (Application Layer)
│   └── search/
│       ├── usecase/
│       │   ├── SearchDocumentsUseCase.java  # Orchestrates domain logic
│       │   └── GetSuggestionsUseCase.java
│       ├── port/
│       │   └── output/
│       │       └── SearchCachePort.java     # Interface for cache
│       └── dto/
│           ├── SearchRequestDTO.java
│           └── SearchResponseDTO.java
│
├── 🟨 infrastructure/                       # Adapters (Infrastructure Layer)
│   ├── persistence/
│   │   ├── jpa/                             # PostgreSQL Adapters
│   │   │   ├── entity/
│   │   │   │   └── CrawlUrlJpaEntity.java   # JPA entity (NOT domain!)
│   │   │   ├── repository/
│   │   │   │   └── CrawlUrlJpaRepository.java # Spring Data JPA
│   │   │   └── adapter/
│   │   │       └── CrawlJobRepositoryJpaAdapter.java # Implements domain interface
│   │   │
│   │   └── elasticsearch/                   # Elasticsearch Adapters
│   │       ├── document/
│   │       │   └── WebPageEsDocument.java   # ES document (NOT domain!)
│   │       ├── repository/
│   │       │   └── WebPageEsRepository.java # Spring Data ES
│   │       └── adapter/
│   │           ├── SearchRepositoryElasticsearchAdapter.java
│   │           └── WebDocumentRepositoryElasticsearchAdapter.java
│   │
│   ├── cache/
│   │   └── redis/adapter/
│   │       └── SearchCacheRedisAdapter.java # Implements SearchCachePort
│   │
│   └── config/
│       ├── CleanArchitectureConfig.java     # Dependency wiring
│       ├── RedisConfig.java
│       ├── CorsConfig.java
│       └── OpenApiConfig.java
│
├── 🟧 presentation/                         # Controllers (Presentation Layer)
│   └── rest/controller/
│       └── SearchControllerV2.java          # Delegates to use cases
│
└── 🟪 shared/                               # Shared Kernel
    └── common/exception/
        └── DomainException.java
```

---

## 🎯 Clean Architecture Layers

### 1. Domain Layer (`domain/`)
**Framework-FREE business logic**

- **No dependencies** on Spring, JPA, Elasticsearch, or any framework
- Contains pure Java POJOs
- Defines business rules and invariants

**What's here:**
- Value Objects (immutable, self-validating)
- Entities (identified by ID)
- Aggregates (cluster of entities)
- Domain Events
- Repository Interfaces (ports)
- Domain Services
- Strategy Interfaces

### 2. Application Layer (`application/`)
**Use cases and orchestration**

- Depends ONLY on domain layer
- Orchestrates domain objects to fulfill use cases
- Defines ports (interfaces) for infrastructure

**What's here:**
- Use Cases (business workflows)
- Application DTOs (data transfer)
- Output Ports (interfaces for infrastructure)

### 3. Infrastructure Layer (`infrastructure/`)
**Technical implementation details**

- Implements domain repository interfaces
- Implements application ports
- Contains framework-specific code (Spring, JPA, etc.)

**What's here:**
- Database adapters (JPA, Elasticsearch)
- Cache adapters (Redis)
- Messaging adapters (Kafka)
- Configuration classes

### 4. Presentation Layer (`presentation/`)
**External interfaces**

- REST controllers
- Delegates to application use cases
- Maps API DTOs ↔ Application DTOs

---

## 📐 Domain-Driven Design Patterns

### Value Objects
**Immutable, self-validating objects identified by their value**

```java
// domain/search/model/valueobject/SearchQuery.java
public final class SearchQuery {
    private final String value;
    
    private SearchQuery(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        this.value = normalize(value);
    }
    
    public static SearchQuery of(String value) {
        return new SearchQuery(value);
    }
}
```

**Examples:**
- `SearchQuery`, `SearchScore`, `Pagination`
- `Url`, `CrawlDepth`, `CrawlStatus`
- `ContentHash`, `PageRankScore`

### Aggregates
**Cluster of entities treated as a single unit**

```java
// domain/crawling/model/aggregate/CrawlJob.java
public class CrawlJob { // Aggregate Root
    private final String id;
    private final Url url;
    private final CrawlDepth depth;
    private CrawlStatus status;
    
    public void markAsCompleted() {
        if (this.status != CrawlStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete in-progress crawls");
        }
        this.status = CrawlStatus.COMPLETED;
        this.publishEvent(new CrawlCompletedEvent(this.id));
    }
}
```

**Examples:**
- `CrawlJob` (manages crawl lifecycle)
- `WebDocument` (manages document indexing)
- `PageGraph` (manages PageRank calculation)

### Domain Events
**Something that happened in the domain**

```java
// domain/crawling/event/CrawlCompletedEvent.java
public record CrawlCompletedEvent(
    String crawlJobId,
    String url,
    LocalDateTime occurredAt
) {}
```

### Repository Interfaces (Ports)
**Defined in domain, implemented in infrastructure**

```java
// domain/search/repository/SearchRepository.java (INTERFACE)
public interface SearchRepository {
    List<SearchResultEntity> search(SearchQuery query, Pagination pagination);
    long countResults(SearchQuery query);
}

// infrastructure/.../SearchRepositoryElasticsearchAdapter.java (IMPLEMENTATION)
@Component
public class SearchRepositoryElasticsearchAdapter implements SearchRepository {
    // Elasticsearch-specific implementation
}
```

### Domain Services
**Business logic that doesn't belong to a single entity**

```java
// domain/ranking/service/PageRankCalculator.java
public class PageRankCalculator {
    public void calculate(PageGraph graph) {
        // PageRank algorithm implementation
    }
}
```

---

## 🔄 Dependency Flow

### Clean Architecture Rules

```
✅ Allowed:
   Presentation → Application → Domain
   Infrastructure → Domain (implements interfaces)

❌ NOT Allowed:
   Domain → Application
   Domain → Infrastructure
```

### Example: Search Request Flow

```
1. HTTP Request
   ↓
2. SearchControllerV2 (presentation)
   ↓ delegates to
3. SearchDocumentsUseCase (application)
   ↓ uses
4. SearchRepository interface (domain)
   ↑ implemented by
5. SearchRepositoryElasticsearchAdapter (infrastructure)
   ↓ queries
6. Elasticsearch
```

---

## 🚀 Reading Roadmap

### Level 1: Understanding Architecture
1. [SearchEngineApplication.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/SearchEngineApplication.java) - Entry point
2. [CleanArchitectureConfig.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/infrastructure/config/CleanArchitectureConfig.java) - Dependency wiring

### Level 2: Domain Layer (Business Logic)
3. [SearchQuery.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/domain/search/model/valueobject/SearchQuery.java) - Value Object example
4. [SearchRepository.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/domain/search/repository/SearchRepository.java) - Repository interface
5. [CrawlJob.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/domain/crawling/model/aggregate/CrawlJob.java) - Aggregate Root
6. [PageRankCalculator.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/domain/ranking/service/PageRankCalculator.java) - Domain Service

### Level 3: Application Layer (Use Cases)
7. [SearchDocumentsUseCase.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/application/search/usecase/SearchDocumentsUseCase.java) - Use case implementation
8. [SearchCachePort.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/application/search/port/output/SearchCachePort.java) - Output port

### Level 4: Infrastructure Layer (Adapters)
9. [SearchRepositoryElasticsearchAdapter.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/infrastructure/persistence/elasticsearch/adapter/SearchRepositoryElasticsearchAdapter.java) - Repository implementation
10. [SearchCacheRedisAdapter.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/infrastructure/cache/redis/adapter/SearchCacheRedisAdapter.java) - Cache adapter

### Level 5: Presentation Layer (API)
11. [SearchControllerV2.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/presentation/rest/controller/SearchControllerV2.java) - REST controller

---

## 💡 Key Concepts

### Dependency Inversion Principle

**❌ Before (Tight coupling):**
```java
public class SearchService {
    private ElasticsearchOperations es; // Depends on infrastructure!
}
```

**✅ After (Dependency Inversion):**
```java
// Domain defines interface
public interface SearchRepository {
    List<SearchResultEntity> search(...);
}

// Application uses interface
public class SearchDocumentsUseCase {
    private final SearchRepository repository; // Depends on abstraction!
}

// Infrastructure implements interface
public class SearchRepositoryElasticsearchAdapter implements SearchRepository {
    private ElasticsearchOperations es; // Infrastructure detail hidden
}
```

### Ports & Adapters

**Port**: Interface defined by application/domain
**Adapter**: Infrastructure implementation

```java
// Port (application/search/port/output/SearchCachePort.java)
public interface SearchCachePort {
    SearchResponseDTO get(String key);
    void put(String key, SearchResponseDTO value, int ttl);
}

// Adapter (infrastructure/cache/redis/adapter/SearchCacheRedisAdapter.java)
@Component
public class SearchCacheRedisAdapter implements SearchCachePort {
    private final RedisTemplate<String, Object> redis;
    // Redis-specific implementation
}
```

### Value Object Benefits

```java
// ✅ Validation at construction
SearchQuery query = SearchQuery.of(""); // throws IllegalArgumentException

// ✅ Immutability
query.setValue("new"); // Compile error - no setter exists

// ✅ Domain logic encapsulated
Pagination page = Pagination.of(0, 10);
Pagination next = page.nextPage(); // Business logic in domain
```

---

## ✨ Benefits of This Architecture

| Benefit | How |
|---------|-----|
| **Testability** | Mock repository interfaces, test domain without infrastructure |
| **Framework Independence** | Domain has ZERO Spring dependencies |
| **Flexibility** | Swap Elasticsearch → Solr by creating new adapter |
| **Maintainability** | Clear boundaries, easy to find code |
| **Scalability** | Each bounded context could become a microservice |

### Testing Example

```java
// Unit test WITHOUT Spring, Elasticsearch, or any infrastructure!
@Test
void shouldValidateSearchQuery() {
    // Given
    String invalidQuery = "";
    
    // When/Then
    assertThrows(IllegalArgumentException.class, 
        () -> SearchQuery.of(invalidQuery));
}

@Test
void shouldSearchDocuments() {
    // Given
    SearchRepository mockRepo = mock(SearchRepository.class);
    SearchCachePort mockCache = mock(SearchCachePort.class);
    SearchDocumentsUseCase useCase = new SearchDocumentsUseCase(mockRepo, mockCache);
    
    // When
    SearchResponseDTO result = useCase.execute(new SearchRequestDTO("test", 0, 10, "relevance"));
    
    // Then - no Spring container, no Elasticsearch needed!
    assertNotNull(result);
}
```

---

## 🎓 Next Steps

1. **Explore Domain Layer** - Start with value objects, understand immutability
2. **Read Use Cases** - See how domain objects are orchestrated
3. **Study Adapters** - Learn how infrastructure implements domain interfaces
4. **Run Tests** - See Clean Architecture benefits in action
5. **Extend** - Add new bounded contexts following the same pattern

---

## 📚 Further Reading

- Clean Architecture by Robert C. Martin
- Domain-Driven Design by Eric Evans
- Implementing Domain-Driven Design by Vaughn Vernon
- Hexagonal Architecture (Ports & Adapters) by Alistair Cockburn

---

**This is TRUE Clean Architecture + DDD!** 🎉
