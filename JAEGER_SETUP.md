# Jaeger Distributed Tracing - Setup Guide

## Overview
This guide shows how to add Jaeger distributed tracing to the search engine for production observability.

## 📋 Prerequisites
- Docker (for local Jaeger instance)
- Maven for dependency management

## 🔧 Step 1: Add Dependencies to pom.xml

Add these dependencies to `search-engine/pom.xml` (before `</dependencies>`):

```xml
<!-- OpenTelemetry (Distributed Tracing with Jaeger) -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
    <version>1.32.0</version>
</dependency>
```

## 🔧 Step 2: Create Tracing Configuration

Create `TracingConfig.java`:

```java
package com.chibao.edu.search_engine.infrastructure.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Value("${tracing.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Value("${spring.application.name:search-engine}")
    private String serviceName;

    @Bean
    public OpenTelemetry openTelemetry() {
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaegerEndpoint)
                .build();

        Resource resource = Resource.getDefault()
                .merge(Resource.create(
                        io.opentelemetry.api.common.Attributes.of(
                                ResourceAttributes.SERVICE_NAME, serviceName
                        )
                ));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }
}
```

Create `TracingService.java`:

```java
package com.chibao.edu.search_engine.infrastructure.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TracingService {

    private final Tracer tracer;

    public Span startSpan(String operationName) {
        return tracer.spanBuilder(operationName).startSpan();
    }

    public <T> T trace(String operationName, TracedOperation<T> operation) {
        Span span = startSpan(operationName);
        try (Scope scope = span.makeCurrent()) {
            T result = operation.execute(span);
            span.setAttribute("success", true);
            return result;
        } catch (Exception e) {
            span.setAttribute("error", true);
            span.setAttribute("error.message", e.getMessage());
            span.recordException(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    public void addAttribute(String key, String value) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    @FunctionalInterface
    public interface TracedOperation<T> {
        T execute(Span span) throws Exception;
    }
}
```

## 🔧 Step 3: Configure Application Properties

Add to `application.properties`:

```properties
# Jaeger Tracing
tracing.jaeger.endpoint=http://localhost:14250
tracing.enabled=true
```

## 🐳 Step 4: Start Jaeger (Docker)

```bash
docker-compose -f docker-compose.jaeger.yml up -d
```

Access Jaeger UI at: http://localhost:16686

## ☸️ Step 5: Deploy to Kubernetes (Optional)

```bash
kubectl apply -f k8s/jaeger.yaml
```

## 📊 Step 6: Use Tracing in Code

```java
@Service
@RequiredArgsConstructor
public class MyCrawlService {
    
    private final TracingService tracingService;
    
    public void crawlPage(String url) {
        tracingService.trace("crawl-page", span -> {
            span.setAttribute("url", url);
            
            // Your crawl logic here
            fetchPage(url);
            
            span.addEvent("page-fetched");
            return null;
        });
    }
}
```

## 🎯 What Gets Traced

Once implemented, you'll see traces for:
- **Crawl Operations**: URL fetching, HTML parsing, link extraction
- **Search Queries**: Query processing, Elasticsearch calls, result ranking
- **PageRank Calculations**: Graph building, iteration convergence
- **Kafka Messages**: Producer/consumer operations

## 📈 Benefits

- **Performance Monitoring**: See exactly where time is spent
- **Error Tracking**: Trace errors across distributed components
- **Dependency Analysis**: Understand service dependencies
- **Bottleneck Identification**: Find slow operations

## 🔗 Files Included

- ✅ `docker-compose.jaeger.yml` - local Jaeger deployment
- ✅ `k8s/jaeger.yaml` - Kubernetes deployment
- ✅ `jaeger.properties` - configuration

## 📚 Resources

- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Distributed Tracing Guide](https://microservices.io/patterns/observability/distributed-tracing.html)

---

**Note**: This is optional but highly recommended for production deployments!
