import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

export interface SearchResult {
    url: string;
    title: string;
    snippet: string;
    score: number;
    domain?: string;
}

export interface SearchResponse {
    query: string;
    totalResults: number;
    executionTimeMs: number;
    results: SearchResult[];
    currentPage: number;
    totalPages: number;
}

export interface Suggestion {
    text: string;
    score: number;
}

// Search API
export const searchApi = {
    search: async (query: string, page: number = 0, size: number = 10): Promise<SearchResponse> => {
        const response = await apiClient.get('/api/v1/search', {
            params: { q: query, page, size },
        });
        return response.data;
    },

    getSuggestions: async (prefix: string): Promise<Suggestion[]> => {
        const response = await apiClient.get('/api/v1/search/suggestions', {
            params: { prefix },
        });
        return response.data;
    },
};

// Admin API
export const adminApi = {
    addSeedUrls: async (urls: string[]) => {
        const response = await apiClient.post('/api/v1/admin/crawl/seeds', urls);
        return response.data;
    },

    getCrawlerStats: async () => {
        const response = await apiClient.get('/api/v1/admin/stats/crawler');
        return response.data;
    },

    getPageRankStats: async () => {
        const response = await apiClient.get('/api/v1/admin/pagerank/stats');
        return response.data;
    },

    triggerPageRank: async () => {
        const response = await apiClient.post('/api/v1/admin/indexer/pagerank/update');
        return response.data;
    },
};

export default apiClient;
