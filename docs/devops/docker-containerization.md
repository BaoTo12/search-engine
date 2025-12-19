# Docker Containerization

> **Production-ready Docker configuration with multi-stage builds and optimization**

---

## Multi-Stage Dockerfile

### Backend (Spring Boot)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first (caching layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Security: Run as non-root
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### Frontend (Next.js)

```dockerfile
# Stage 1: Dependencies
FROM node:18-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --only=production

# Stage 2: Build
FROM node:18-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 3: Runtime
FROM node:18-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]
```

---

## Docker Compose (Development)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: crawler-postgres
    environment:
      POSTGRES_DB: crawler_db
      POSTGRES_USER: crawler_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - search-engine-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U crawler_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: crawler-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - search-engine-network
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: crawler-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - search-engine-network

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: crawler-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    networks:
      - search-engine-network

  backend:
    build:
      context: ./search-engine
      dockerfile: Dockerfile
    container_name: search-engine-backend
    depends_on:
      - postgres
      - redis
      - kafka
      - elasticsearch
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/crawler_db
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      ELASTICSEARCH_URL: http://elasticsearch:9200
    networks:
      - search-engine-network
    restart: unless-stopped

  frontend:
    build:
      context: ./search-engine-ui
      dockerfile: Dockerfile
    container_name: search-engine-frontend
    depends_on:
      - backend
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080
    networks:
      - search-engine-network

volumes:
  postgres_data:
  redis_data:
  es_data:

networks:
  search-engine-network:
    driver: bridge
```

---

## Optimization Techniques

### 1. Layer Caching

```dockerfile
# BAD: Changes to ANY file rebuilds dependencies
COPY . .
RUN mvn clean package

# GOOD: Cache dependencies separately
COPY pom.xml .
RUN mvn dependency:go-offline  # Cached if pom.xml unchanged
COPY src ./src
RUN mvn package -DskipTests
```

### 2. Multi-Stage Benefits

```
Single-stage JAR: 450 MB
Multi-stage JRE: 180 MB (60% reduction!)

Build tools not included in runtime image
```

### 3. Alpine Linux

```dockerfile
FROM eclipse-temurin:21-jre-alpine  # 180 MB
# vs
FROM eclipse-temurin:21-jre         # 450 MB
```

### 4. .dockerignore

```
# .dockerignore
target/
node_modules/
.git/
*.log
.env
*.md
docs/
```

---

## Security Best Practices

### 1. Non-Root User

```dockerfile
# Create dedicated user
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

USER appuser  # Don't run as root!
```

### 2. Scan for Vulnerabilities

```bash
# Scan image
docker scan search-engine-backend:latest

# Or use Trivy
trivy image search-engine-backend:latest
```

### 3. Use Specific Tags

```dockerfile
# BAD
FROM openjdk:latest

# GOOD
FROM eclipse-temurin:21-jre-alpine
```

---

## Production Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS production

LABEL maintainer="your-email@example.com"
LABEL version="1.0.0"
LABEL description="Search Engine Backend"

WORKDIR /app

# Install dumb-init (proper signal handling)
RUN apk add --no-cache dumb-init

# Create user
RUN addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser && \
    chown -R appuser:appgroup /app

# Copy JAR
COPY --chown=appuser:appgroup target/*.jar app.jar

# Switch to non-root
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep UP || exit 1

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

CMD ["java", \
    "-server", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

---

*Multi-stage Docker builds create secure, optimized production images!*
