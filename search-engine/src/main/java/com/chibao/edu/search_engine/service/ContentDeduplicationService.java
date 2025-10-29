package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.entity.WebPage;
import com.chibao.edu.search_engine.repository.WebPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Senior Level: Content-Based Deduplication using SimHash
 *
 * SimHash is a locality-sensitive hashing technique that:
 * - Generates fixed-size fingerprints for documents
 * - Similar documents have similar fingerprints (small Hamming distance)
 * - Allows detection of near-duplicate content (not just exact URL duplicates)
 *
 * Use Cases:
 * - Detect mirror sites or scraped content
 * - Identify URL aliases (same content, different URLs)
 * - Reduce index bloat from duplicate content
 * - Improve search quality by filtering duplicates
 *
 * Algorithm:
 * 1. Tokenize document into features (words with tf-idf weights)
 * 2. Hash each feature to get a 64-bit hash
 * 3. Build weighted vector and collapse to fingerprint
 * 4. Compare fingerprints using Hamming distance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDeduplicationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final WebPageRepository webPageRepository;
    private final Analyzer analyzer = new EnglishAnalyzer();

    private static final String SIMHASH_KEY_PREFIX = "simhash:";
    private static final int SIMHASH_BITS = 64;
    private static final int HAMMING_DISTANCE_THRESHOLD = 3; // Max bits difference for duplicates
    private static final int MIN_CONTENT_LENGTH = 100; // Minimum chars to compute hash
    private static final long CACHE_TTL_DAYS = 30;

    /**
     * Compute SimHash fingerprint for content
     */
    public long computeSimHash(String content) {
        if (content == null || content.length() < MIN_CONTENT_LENGTH) {
            return 0L;
        }

        try {
            // Extract weighted features (terms with frequencies)
            Map<String, Integer> termFrequencies = extractTermFrequencies(content);

            if (termFrequencies.isEmpty()) {
                return 0L;
            }

            // Initialize vector for weighted hash accumulation
            int[] vector = new int[SIMHASH_BITS];

            // Process each term
            for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
                String term = entry.getKey();
                int frequency = entry.getValue();

                // Hash the term
                long hash = hashTerm(term);

                // Weight the hash by term frequency
                for (int i = 0; i < SIMHASH_BITS; i++) {
                    // Check if bit i is set in the hash
                    if (((hash >> i) & 1) == 1) {
                        vector[i] += frequency;
                    } else {
                        vector[i] -= frequency;
                    }
                }
            }

            // Collapse vector to final fingerprint
            long fingerprint = 0L;
            for (int i = 0; i < SIMHASH_BITS; i++) {
                if (vector[i] > 0) {
                    fingerprint |= (1L << i);
                }
            }

            return fingerprint;

        } catch (Exception e) {
            log.error("Error computing SimHash", e);
            return 0L;
        }
    }

    /**
     * Extract term frequencies from content using Lucene analyzer
     */
    private Map<String, Integer> extractTermFrequencies(String content) {
        Map<String, Integer> frequencies = new HashMap<>();

        try (TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(content))) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString();

                // Only consider terms of reasonable length
                if (term.length() >= 3 && term.length() <= 20) {
                    frequencies.merge(term, 1, Integer::sum);
                }
            }

            tokenStream.end();

        } catch (Exception e) {
            log.error("Error extracting term frequencies", e);
        }

        return frequencies;
    }

    /**
     * Hash a term to 64-bit long
     */
    private long hashTerm(String term) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(term.getBytes());

            // Convert first 8 bytes to long
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }

            return result;

        } catch (Exception e) {
            // Fallback to simple hash
            return term.hashCode();
        }
    }

    /**
     * Calculate Hamming distance between two fingerprints
     * (number of differing bits)
     */
    public int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }

    /**
     * Check if content is a duplicate of existing content
     * Returns the URL of duplicate if found, null otherwise
     */
    public String findDuplicate(String content, String currentUrl) {
        long fingerprint = computeSimHash(content);

        if (fingerprint == 0L) {
            return null;
        }

        // Check Redis cache for similar fingerprints
        Set<String> candidateKeys = redisTemplate.keys(SIMHASH_KEY_PREFIX + "*");

        if (candidateKeys == null || candidateKeys.isEmpty()) {
            // No existing fingerprints
            storeFingerprint(currentUrl, fingerprint);
            return null;
        }

        // Check each candidate for similarity
        for (String key : candidateKeys) {
            try {
                String storedData = redisTemplate.opsForValue().get(key);
                if (storedData == null) continue;

                String[] parts = storedData.split(":", 2);
                if (parts.length != 2) continue;

                long storedFingerprint = Long.parseLong(parts[0]);
                String storedUrl = parts[1];

                // Skip if it's the same URL
                if (storedUrl.equals(currentUrl)) {
                    continue;
                }

                // Calculate similarity
                int distance = hammingDistance(fingerprint, storedFingerprint);

                if (distance <= HAMMING_DISTANCE_THRESHOLD) {
                    log.info("Found duplicate content: {} (distance: {}) is similar to {}",
                            currentUrl, distance, storedUrl);
                    return storedUrl;
                }

            } catch (Exception e) {
                log.error("Error checking fingerprint similarity", e);
            }
        }

        // No duplicate found, store this fingerprint
        storeFingerprint(currentUrl, fingerprint);
        return null;
    }

    /**
     * Store fingerprint in Redis
     */
    private void storeFingerprint(String url, long fingerprint) {
        try {
            String key = SIMHASH_KEY_PREFIX + url.hashCode();
            String value = fingerprint + ":" + url;

            redisTemplate.opsForValue().set(key, value, CACHE_TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.error("Error storing fingerprint for {}", url, e);
        }
    }

    /**
     * Batch find duplicates in Elasticsearch index
     * Useful for cleaning up existing index
     */
    public List<DuplicateGroup> findDuplicatesInIndex(int batchSize) {
        log.info("Starting duplicate detection in index...");

        Map<Long, List<String>> fingerprintGroups = new HashMap<>();

        // Fetch all documents (in production, do this in batches)
        Iterable<WebPage> pages = webPageRepository.findAll();

        int processed = 0;
        for (WebPage page : pages) {
            long fingerprint = computeSimHash(page.getContent());

            fingerprintGroups
                    .computeIfAbsent(fingerprint, k -> new ArrayList<>())
                    .add(page.getUrl());

            processed++;
            if (processed % batchSize == 0) {
                log.info("Processed {} documents for duplicate detection", processed);
            }
        }

        // Find groups with near-duplicate fingerprints
        List<DuplicateGroup> duplicates = new ArrayList<>();

        List<Map.Entry<Long, List<String>>> entries = new ArrayList<>(fingerprintGroups.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Long, List<String>> entry1 = entries.get(i);

            // Check against subsequent entries
            for (int j = i + 1; j < entries.size(); j++) {
                Map.Entry<Long, List<String>> entry2 = entries.get(j);

                int distance = hammingDistance(entry1.getKey(), entry2.getKey());

                if (distance <= HAMMING_DISTANCE_THRESHOLD) {
                    List<String> allUrls = new ArrayList<>();
                    allUrls.addAll(entry1.getValue());
                    allUrls.addAll(entry2.getValue());

                    duplicates.add(new DuplicateGroup(allUrls, distance));
                }
            }
        }

        log.info("Found {} duplicate groups in index", duplicates.size());
        return duplicates;
    }

    /**
     * Remove duplicate documents from index
     * Keeps the URL with highest PageRank or earliest crawl date
     */
    public int removeDuplicates(List<DuplicateGroup> duplicates) {
        int removed = 0;

        for (DuplicateGroup group : duplicates) {
            try {
                // Fetch all documents in group
                List<WebPage> pages = group.getUrls().stream()
                        .map(url -> webPageRepository.findById(generateDocId(url)))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                if (pages.size() <= 1) {
                    continue;
                }

                // Find best candidate to keep
                WebPage toKeep = pages.stream()
                        .max(Comparator.comparing(WebPage::getPageRank))
                        .orElse(pages.get(0));

                // Remove others
                for (WebPage page : pages) {
                    if (!page.getId().equals(toKeep.getId())) {
                        webPageRepository.delete(page);
                        removed++;
                        log.debug("Removed duplicate: {} (kept: {})", page.getUrl(), toKeep.getUrl());
                    }
                }

            } catch (Exception e) {
                log.error("Error removing duplicates for group", e);
            }
        }

        log.info("Removed {} duplicate documents", removed);
        return removed;
    }

    /**
     * Generate document ID (same as in IndexerService)
     */
    private String generateDocId(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }

    /**
     * Calculate content similarity percentage
     */
    public double calculateSimilarity(String content1, String content2) {
        long hash1 = computeSimHash(content1);
        long hash2 = computeSimHash(content2);

        int distance = hammingDistance(hash1, hash2);

        // Convert to similarity percentage
        return (1.0 - (double) distance / SIMHASH_BITS) * 100.0;
    }

    /**
     * Get deduplication statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        Set<String> keys = redisTemplate.keys(SIMHASH_KEY_PREFIX + "*");
        stats.put("cached_fingerprints", keys != null ? keys.size() : 0);
        stats.put("hamming_threshold", HAMMING_DISTANCE_THRESHOLD);
        stats.put("simhash_bits", SIMHASH_BITS);

        return stats;
    }

    /**
     * Data class for duplicate groups
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DuplicateGroup {
        private List<String> urls;
        private int hammingDistance;
    }
}