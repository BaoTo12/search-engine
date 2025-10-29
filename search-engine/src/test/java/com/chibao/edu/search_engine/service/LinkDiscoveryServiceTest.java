package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class LinkDiscoveryServiceTest {

    @Mock
    private CrawlUrlRepository crawlUrlRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private LinkDiscoveryService linkDiscoveryService;

    // Additional test methods would go here
}

