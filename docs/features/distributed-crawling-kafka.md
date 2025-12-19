# Distributed Crawling with Kafka

> **File:** Kafka integration across services  
> **Pattern:** Event-driven architecture with domain partitioning

---

## Kafka Topics Architecture

### Topic Structure

```
crawl-requests (10 partitions)
├─ Partition 0: domain hash % 10 = 0
├─ Partition 1: domain hash % 10 = 1
├─ ...
└─ Partition 9: domain hash % 10 = 9

pages (10 partitions)
├─ URL-based partitioning
└─ For parallel indexing

new-links (5 partitions)
├─ Source domain partitioning
└─ For link discovery
```

---

## Domain-Based Partitioning

### Why Partition by Domain?

**Problem:** Multiple crawlers hitting same domain = rate limit violations

**Solution:** Same domain always goes to same consumer

```
example.com → hash → partition 3 → consumer C3
example.com → hash → partition 3 → consumer C3 (always!)

Benefits:
1. Natural rate limiting (one consumer per domain)
2. No distributed coordination needed
3. Automatic load balancing
```

### Implementation

```java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, CrawlRequest> crawlRequestProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Custom partitioner
        config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DomainPartitioner.class);
        
        // Performance tuning
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 32KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);      // Wait 10ms to batch
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.ACKS_CONFIG, "1");          // Leader ack
        
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Custom Partitioner

```java
public class DomainPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        String domain = (String) key;
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Consistent hashing: same domain → same partition
        return Math.abs(domain.hashCode() % numPartitions);
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

## Producer (Scheduler → Kafka)

### Dispatching URLs

```java
@Service
public class CrawlSchedulerService {
    
    @Autowired
    private KafkaTemplate<String, CrawlRequest> kafkaTemplate;
    
    @Scheduled(fixedDelay = 10000)  // Every 10 seconds
    @Transactional
    public void dispatchPendingUrls() {
        List<CrawlUrl> pending = crawlUrlRepository
            .findByStatusOrderByPriorityDesc(CrawlStatus.PENDING, PageRequest.of(0, 100));
        
        for (CrawlUrl url : pending) {
            // Check rate limiting
            if (rateLimiter.isAllowed(url.getDomain())) {
                
                CrawlRequest request = CrawlRequest.builder()
                    .url(url.getUrl())
                    .domain(url.getDomain())
                    .depth(url.getDepth())
                    .priority(url.getPriority())
                    .build();
                
                // Send with domain as key (for partitioning)
                kafkaTemplate.send("crawl-requests", url.getDomain(), request);
                
                // Update status
                url.setStatus(CrawlStatus.IN_PROGRESS);
                crawlUrlRepository.save(url);
            }
        }
    }
}
```

---

## Consumer (Kafka → Crawler Workers)

### Configuration

```java
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, CrawlRequest> crawlRequestConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "crawler-workers");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Start from earliest on first run
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Manual commit for exactly-once
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Fetch configuration
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CrawlRequest>
            crawlRequestListenerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, CrawlRequest> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(crawlRequestConsumerFactory());
        factory.setConcurrency(10);  // 10 parallel consumers
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        
        return factory;
    }
}
```

### Crawler Worker

```java
@Service
public class CrawlerWorkerService {
    
    @KafkaListener(
        topics = "crawl-requests",
        groupId = "crawler-workers",
        containerFactory = "crawlRequestListenerFactory"
    )
    public void processCrawlRequest(
        @Payload CrawlRequest request,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        Acknowledgment ack
    ) {
        log.info("Processing URL: {} (partition: {})", request.getUrl(), partition);
        
        try {
            // 1. Check if already processed (idempotency)
            if (isDuplicate(request.getUrl())) {
                ack.acknowledge();
                return;
            }
            
            // 2. Check robots.txt
            if (!robotsTxtService.isAllowed(request.getDomain(), request.getUrl())) {
                markAsBlocked(request.getUrl());
                ack.acknowledge();
                return;
            }
            
            // 3. Fetch page
            HttpResponse<String> response = fetchPage(request.getUrl());
            
            // 4. Parse HTML
            Document doc = Jsoup.parse(response.body());
            String title = doc.title();
            String content = doc.body().text();
            List<String> links = extractLinks(doc, request.getUrl());
  
            // 5. Send to indexer
            WebPageDTO page = WebPageDTO.builder()
                .url(request.getUrl())
                .title(title)
                .content(content)
                .domain(request.getDomain())
                .crawledAt(LocalDateTime.now())
                .build();
            
            kafkaTemplate.send("pages", request.getUrl(), page);
            
            // 6. Send discovered links
            if (!links.isEmpty()) {
                LinkDiscoveryEvent event = LinkDiscoveryEvent.builder()
                    .sourceUrl(request.getUrl())
                    .discoveredLinks(links)
                    .depth(request.getDepth() + 1)
                    .build();
                
                kafkaTemplate.send("new-links", request.getDomain(), event);
            }
            
            // 7. Update database
            markAsCompleted(request.getUrl());
            
            // 8. Commit offset (exactly-once)
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to crawl: {}", request.getUrl(), e);
            
            // Don't ack - message will be redelivered
            if (getRetryCount(request) < MAX_RETRIES) {
                // Will be retried
            } else {
                sendToDeadLetterQueue(request);
                ack.acknowledge();
            }
        }
    }
}
```

---

## Exactly-Once Semantics

### Problem: Duplicate Processing

```
Scenario:
1. Consumer fetches message
2. Processes URL successfully
3. Crashes before committing offset
4. Message redelivered → duplicate processing
```

### Solution: Idempotent Processing

```java
public boolean isDuplicate(String url) {
    // Layer 1: Bloom filter (fast)
    if (!bloomFilter.mightContain(url)) {
        return false;
    }
    
    // Layer 2: Database check
    String urlHash = hashUrl(url);
    return crawlUrlRepository.existsByUrlHash(urlHash);
}

@Transactional
public void processPage(CrawlRequest request, Acknowledgment ack) {
    // 1. Check duplicate
    if (isDuplicate(request.getUrl())) {
        ack.acknowledge();
        return;
    }
    
    // 2. Process
    WebPage page = crawlAndParse(request);
    
    // 3. Save to DB + mark in Bloom filter
    webPageRepository.save(page);
    bloomFilter.put(request.getUrl());
    
    // 4. Commit Kafka offset
    ack.acknowledge();
}
```

---

## Consumer Group Rebalancing

### Scenario: Consumer Crashes

```
Before crash:
Consumer 1: partitions [0, 1, 2]
Consumer 2: partitions [3, 4, 5]
Consumer 3: partitions [6, 7, 8, 9]

Consumer 2 crashes →

After rebalance:
Consumer 1: partitions [0, 1, 2, 3, 4]
Consumer 3: partitions [5, 6, 7, 8, 9]
```

**Automatic!** No manual intervention needed.

---

## Performance Tuning

### Producer Batching

```java
// Trade latency for throughput
config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);      // 32KB batches
config.put(ProducerConfig.LINGER_MS_CONFIG, 10);          // Wait 10ms
config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

Result:
- 10x fewer network calls
- 3x higher throughput
- +10ms latency (acceptable)
```

### Consumer Fetch Configuration

```java
config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 10240);  // 10KB minimum
config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);  // Wait 500ms

Result:
- Batch processing
- Reduced network overhead
```

---

## Monitoring

### Key Metrics

```java
// Producer metrics
kafka.producer.record-send-rate
kafka.producer.batch-size-avg
kafka.producer.compression-rate-avg

// Consumer metrics
kafka.consumer.records-consumed-rate
kafka.consumer.fetch-latency-avg
kafka.consumer.lag

// Topic metrics
kafka.topic.partition-count
kafka.topic.bytes-in-per-sec
```

### Consumer Lag Monitoring

```bash
# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group crawler-workers \
  --describe

TOPIC            PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
crawl-requests   0          12543          12543           0
crawl-requests   1          11234          11300           66    ← lagging!
crawl-requests   2          13421          13421           0
```

**Alert if lag > 1000 for 5 minutes**

---

*Domain-partitioned Kafka enables distributed crawling with automatic coordination!*
