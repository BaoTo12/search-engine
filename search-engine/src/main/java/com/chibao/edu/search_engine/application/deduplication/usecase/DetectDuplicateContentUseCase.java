package com.chibao.edu.search_engine.application.deduplication.usecase;

import com.chibao.edu.search_engine.domain.deduplication.service.SimHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Use case for detecting duplicate content using SimHash.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetectDuplicateContentUseCase {

    private final SimHashService simHashService;

    // In-memory storage for demo (in production, use database or cache)
    private final Map<String, Long> urlToSimHash = new HashMap<>();

    /**
     * Check if content is a near-duplicate of already crawled content.
     * Returns the URL of the duplicate if found, otherwise null.
     */
    public String findDuplicate(String url, String content) {
        // Calculate SimHash for new content
        long newSimHash = simHashService.calculateSimHash(content);

        // Check against existing hashes
        for (Map.Entry<String, Long> entry : urlToSimHash.entrySet()) {
            String existingUrl = entry.getKey();
            long existingSimHash = entry.getValue();

            if (simHashService.areNearDuplicates(newSimHash, existingSimHash)) {
                double similarity = simHashService.calculateSimilarity(newSimHash, existingSimHash);
                log.info("Found near-duplicate: {} similar to {} ({}% similarity)",
                        url, existingUrl, String.format("%.2f", similarity));
                return existingUrl;
            }
        }

        // No duplicate found, store this hash
        urlToSimHash.put(url, newSimHash);
        log.debug("No duplicate found for: {}", url);
        return null;
    }

    /**
     * Calculate similarity between two URLs.
     */
    public double calculateSimilarity(String url1, String url2) {
        Long hash1 = urlToSimHash.get(url1);
        Long hash2 = urlToSimHash.get(url2);

        if (hash1 == null || hash2 == null) {
            return 0.0;
        }

        return simHashService.calculateSimilarity(hash1, hash2);
    }

    /**
     * Get total number of unique documents tracked.
     */
    public int getUniqueDocumentCount() {
        return urlToSimHash.size();
    }

    /**
     * Clear all stored hashes (use with caution).
     */
    public void clear() {
        urlToSimHash.clear();
        log.info("Cleared all SimHash data");
    }
}
