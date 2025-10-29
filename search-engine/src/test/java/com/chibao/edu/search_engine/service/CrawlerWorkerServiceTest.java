package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.dto.request.IndexRequest;
import com.chibao.edu.search_engine.dto.response.CrawlResult;
import com.chibao.edu.search_engine.dto.response.LinkDiscoveryMessage;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.DomainMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class CrawlerWorkerServiceTest {

    @Mock
    private KafkaTemplate<String, CrawlResult> crawlResultKafkaTemplate;

    @Mock
    private KafkaTemplate<String, IndexRequest> indexRequestKafkaTemplate;

    @Mock
    private KafkaTemplate<String, LinkDiscoveryMessage> linkDiscoveryKafkaTemplate;

    @Mock
    private CrawlUrlRepository crawlUrlRepository;

    @Mock
    private DomainMetadataRepository domainMetadataRepository;

    @InjectMocks
    private CrawlerWorkerService crawlerWorkerService;

    @Test
    void testExtractLinks() {
        // This would require JSoup Document mocking
        // Example structure:

        // Given: HTML content with links
        // When: extractLinks is called
        // Then: Should return list of absolute URLs

        // Implementation would use JSoup.parse() with mock HTML
    }
}