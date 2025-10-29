package com.chibao.edu.search_engine.controller;

import com.chibao.edu.search_engine.dto.search.SearchResponse;
import com.chibao.edu.search_engine.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void testSearchEndpoint() throws Exception {
        // Given
        SearchResponse mockResponse = SearchResponse.builder()
                .query("test")
                .totalResults(10L)
                .results(List.of())
                .build();

        when(searchService.search(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("test"))
                .andExpect(jsonPath("$.totalResults").value(10));
    }

    @Test
    void testSuggestionsEndpoint() throws Exception {
        // Given
        List<String> mockSuggestions = List.of("java", "javascript", "java concurrency");
        when(searchService.getSuggestions(anyString())).thenReturn(mockSuggestions);

        // When & Then
        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("prefix", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
