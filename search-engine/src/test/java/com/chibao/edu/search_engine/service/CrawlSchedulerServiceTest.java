package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.dto.request.CrawlRequest;
import com.chibao.edu.search_engine.entity.CrawlUrl;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlSchedulerServiceTest {

    @Mock
    private CrawlUrlRepository crawlUrlRepository;

    @Mock
    private DomainMetadataRepository domainMetadataRepository;

    @Mock
    private KafkaTemplate<String, CrawlRequest> crawlRequestKafkaTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private CrawlSchedulerService crawlSchedulerService;

    @Test
    void testAddSeedUrls() {
        // Given
        List<String> seedUrls = List.of(
                "https://example.com",
                "https://test.com"
        );

        when(crawlUrlRepository.existsByUrlHash(anyString())).thenReturn(false);
        when(crawlUrlRepository.save(any(CrawlUrl.class))).thenAnswer(i -> i.getArgument(0));

        // When
        crawlSchedulerService.addSeedUrls(seedUrls);

        // Then
        verify(crawlUrlRepository, times(2)).save(any(CrawlUrl.class));
    }

    @Test
    void testAddSeedUrls_SkipsDuplicates() {
        // Given
        List<String> seedUrls = List.of("https://example.com");
        when(crawlUrlRepository.existsByUrlHash(anyString())).thenReturn(true);

        // When
        crawlSchedulerService.addSeedUrls(seedUrls);

        // Then
        verify(crawlUrlRepository, never()).save(any(CrawlUrl.class));
    }
}