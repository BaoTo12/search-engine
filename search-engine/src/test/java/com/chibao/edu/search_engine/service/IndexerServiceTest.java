package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.repository.elasticsearch.WebPageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexerServiceTest {

    @Mock
    private WebPageRepository webPageRepository;

    @InjectMocks
    private IndexerService indexerService;

    @Test
    void testTokenizeContent() throws Exception {
        // This would test the Lucene tokenization logic
        // Example: verify stopwords are removed, stemming is applied
    }
}
