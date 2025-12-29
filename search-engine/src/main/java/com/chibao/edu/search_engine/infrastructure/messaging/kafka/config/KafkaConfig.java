package com.chibao.edu.search_engine.infrastructure.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for the Search Engine.
 * Configures topics, producers, and consumers for event-driven architecture.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // =====================================================
    // Topic Definitions
    // =====================================================

    /**
     * Topic for crawl requests. Scheduler publishes URLs to be crawled here.
     */
    @Bean
    public NewTopic crawlRequestsTopic() {
        return TopicBuilder.name("crawl-requests")
                .partitions(16) // High parallelism for distributed crawling
                .replicas(1) // Single replica for dev (increase in production)
                .config("retention.ms", "604800000") // 7 days retention
                .build();
    }

    /**
     * Topic for crawled page content. Workers publish page data here after
     * crawling.
     */
    @Bean
    public NewTopic pageContentTopic() {
        return TopicBuilder.name("pages")
                .partitions(16)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    /**
     * Topic for newly discovered links. Workers publish extracted links here.
     */
    @Bean
    public NewTopic newLinksTopic() {
        return TopicBuilder.name("new-links")
                .partitions(16)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    /**
     * Dead letter queue for failed messages.
     */
    @Bean
    public NewTopic deadLetterQueueTopic() {
        return TopicBuilder.name("dead-letter-queue")
                .partitions(4)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days retention for debugging
                .build();
    }

    // =====================================================
    // Producer Configuration
    // =====================================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once semantics

        // Performance settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // =====================================================
    // Consumer Configuration
    // =====================================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Consumer group settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability

        // Json deserializer settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chibao.edu.search_engine.*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(4); // 4 consumer threads per listener
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
