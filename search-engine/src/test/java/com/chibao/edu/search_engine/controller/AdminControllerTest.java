package com.chibao.edu.search_engine.controller;

import com.chibao.edu.search_engine.common.CrawlStatus;
import com.chibao.edu.search_engine.repository.CrawlUrlRepository;
import com.chibao.edu.search_engine.repository.WebPageRepository;
import com.chibao.edu.search_engine.service.CrawlSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlSchedulerService crawlSchedulerService;

    @MockitoBean
    private CrawlUrlRepository crawlUrlRepository;

    @MockitoBean
    private WebPageRepository webPageRepository;

    @Test
    void testAddSeedUrls() throws Exception {
        // Given
        String requestBody = "[\"https://example.com\", \"https://test.com\"]";

        // When & Then
        mockMvc.perform(post("/api/v1/admin/crawl/seeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.count").value(2));

        verify(crawlSchedulerService).addSeedUrls(anyList());
    }

    @Test
    void testGetCrawlerStats() throws Exception {
        // Given
        when(crawlUrlRepository.countByStatus(CrawlStatus.PENDING)).thenReturn(100L);
        when(crawlUrlRepository.countByStatus(CrawlStatus.COMPLETED)).thenReturn(500L);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/stats/crawler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(100))
                .andExpect(jsonPath("$.completed").value(500));
    }
}