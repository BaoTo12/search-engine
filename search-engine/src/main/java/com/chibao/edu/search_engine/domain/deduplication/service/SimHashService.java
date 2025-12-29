package com.chibao.edu.search_engine.domain.deduplication.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;

/**
 * SimHash implementation for near-duplicate content detection.
 * 
 * SimHash is a locality-sensitive hashing technique:
 * - Similar documents have similar hash values
 * - Can detect near-duplicates by comparing Hamming distance
 * 
 * Algorithm:
 * 1. Tokenize document
 * 2. Hash each token to 64-bit value
 * 3. For each bit position, sum token hashes (weighted)
 * 4. Final hash: bit is 1 if sum > 0, else 0
 */
public class SimHashService {

    private static final int HASH_BITS = 64;
    private static final int SIMILARITY_THRESHOLD = 3; // Hamming distance threshold

    /**
     * Calculate SimHash for a document.
     */
    public long calculateSimHash(String content) {
        if (content == null || content.isEmpty()) {
            return 0L;
        }

        // Initialize vector for bit accumulation
        int[] vector = new int[HASH_BITS];

        // Tokenize and process
        String[] tokens = tokenize(content);
        for (String token : tokens) {
            long tokenHash = hashToken(token);

            // For each bit in the hash
            for (int i = 0; i < HASH_BITS; i++) {
                // If bit is set, add weight; otherwise subtract
                if (((tokenHash >> i) & 1) == 1) {
                    vector[i] += 1;
                } else {
                    vector[i] -= 1;
                }
            }
        }

        // Generate final hash
        long simhash = 0L;
        for (int i = 0; i < HASH_BITS; i++) {
            if (vector[i] > 0) {
                simhash |= (1L << i);
            }
        }

        return simhash;
    }

    /**
     * Calculate Hamming distance between two SimHash values.
     * Returns number of differing bits.
     */
    public int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        return Long.bitCount(xor);
    }

    /**
     * Check if two documents are near-duplicates.
     */
    public boolean areNearDuplicates(long hash1, long hash2) {
        return hammingDistance(hash1, hash2) <= SIMILARITY_THRESHOLD;
    }

    /**
     * Check if two documents are near-duplicates with custom threshold.
     */
    public boolean areNearDuplicates(long hash1, long hash2, int threshold) {
        return hammingDistance(hash1, hash2) <= threshold;
    }

    /**
     * Calculate similarity percentage (0-100).
     */
    public double calculateSimilarity(long hash1, long hash2) {
        int distance = hammingDistance(hash1, hash2);
        return (1.0 - ((double) distance / HASH_BITS)) * 100.0;
    }

    /**
     * Simple tokenization - split by whitespace and lowercase.
     */
    private String[] tokenize(String content) {
        return content.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");
    }

    /**
     * Hash a token to 64-bit value using MD5.
     */
    private long hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));

            // Convert first 8 bytes to long
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }
            return result;

        } catch (Exception e) {
            // Fallback to Java hashCode
            return token.hashCode();
        }
    }

    /**
     * Get bit representation of SimHash (for debugging).
     */
    public String toBinaryString(long simhash) {
        return String.format("%64s", Long.toBinaryString(simhash)).replace(' ', '0');
    }
}
