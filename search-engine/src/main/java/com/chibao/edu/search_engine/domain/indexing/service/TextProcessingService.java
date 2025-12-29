package com.chibao.edu.search_engine.domain.indexing.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for text processing.
 * Provides tokenization, stop word removal, and stemming.
 * 
 * NO framework dependencies - pure domain logic using Lucene analyzers.
 */
public class TextProcessingService {

    /**
     * Process text for indexing: tokenize, remove stop words, and stem.
     */
    public List<String> processText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();

        try (Analyzer analyzer = new EnglishAnalyzer()) {
            TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text));
            tokenStream.reset();

            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                String token = termAttribute.toString();
                if (token.length() > 2 && token.length() < 50) { // Filter very short and very long tokens
                    tokens.add(token);
                }
            }

            tokenStream.end();
            tokenStream.close();

        } catch (Exception e) {
            // Fallback to simple tokenization
            return simpleTokenize(text);
        }

        return tokens;
    }

    /**
     * Custom processing with individual steps for more control.
     */
    public List<String> processWithSteps(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();

        try {
            // Create tokenizer
            StandardTokenizer tokenizer = new StandardTokenizer();
            tokenizer.setReader(new StringReader(text));

            // Apply filters: lowercase -> stop words -> stemming
            TokenStream tokenStream = new LowerCaseFilter(tokenizer);
            tokenStream = new StopFilter(tokenStream, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
            tokenStream = new PorterStemFilter(tokenStream);

            tokenStream.reset();
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                String token = termAttribute.toString();
                if (token.length() > 2 && token.length() < 50) {
                    tokens.add(token);
                }
            }

            tokenStream.end();
            tokenStream.close();

        } catch (Exception e) {
            return simpleTokenize(text);
        }

        return tokens;
    }

    /**
     * Extract keywords from text (most important tokens).
     */
    public List<String> extractKeywords(String text, int maxKeywords) {
        List<String> allTokens = processText(text);

        // Simple frequency-based keyword extraction
        java.util.Map<String, Integer> frequency = new java.util.HashMap<>();
        for (String token : allTokens) {
            frequency.put(token, frequency.getOrDefault(token, 0) + 1);
        }

        return frequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(maxKeywords)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Simple fallback tokenization (no Lucene).
     */
    private List<String> simpleTokenize(String text) {
        return java.util.Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(token -> token.length() > 2)
                .distinct()
                .limit(1000)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate content quality score (0-1).
     */
    public double calculateContentQuality(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        int length = text.length();

        // Length score (0.3 weight)
        double lengthScore = Math.min(1.0, length / 5000.0); // Optimal around 5000 chars
        score += lengthScore * 0.3;

        // Word count score (0.2 weight)
        int wordCount = text.split("\\s+").length;
        double wordScore = Math.min(1.0, wordCount / 800.0); // Optimal around 800 words
        score += wordScore * 0.2;

        // Unique word ratio (0.3 weight)
        List<String> tokens = processText(text);
        long uniqueTokens = tokens.stream().distinct().count();
        double uniqueRatio = tokens.isEmpty() ? 0 : (double) uniqueTokens / tokens.size();
        score += uniqueRatio * 0.3;

        // Average word length (0.2 weight)
        double avgWordLength = tokens.stream()
                .mapToInt(String::length)
                .average()
                .orElse(0);
        double wordLengthScore = Math.min(1.0, avgWordLength / 6.0); // Optimal around 6 chars
        score += wordLengthScore * 0.2;

        return Math.min(1.0, score);
    }
}
