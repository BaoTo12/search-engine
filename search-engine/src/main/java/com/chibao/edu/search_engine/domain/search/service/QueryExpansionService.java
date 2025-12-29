package com.chibao.edu.search_engine.domain.search.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for query expansion.
 * Expands user queries with synonyms and related terms for better recall.
 */
public class QueryExpansionService {

    // Simple synonym dictionary (in production, use WordNet or word embeddings)
    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();

    static {
        // Programming terms
        SYNONYM_MAP.put("java", Arrays.asList("jdk", "jvm", "javac"));
        SYNONYM_MAP.put("python", Arrays.asList("py", "python3", "cpython"));
        SYNONYM_MAP.put("javascript", Arrays.asList("js", "ecmascript", "node"));

        // General terms
        SYNONYM_MAP.put("search", Arrays.asList("find", "lookup", "query"));
        SYNONYM_MAP.put("database", Arrays.asList("db", "datastore", "repository"));
        SYNONYM_MAP.put("algorithm", Arrays.asList("algo", "procedure", "method"));
        SYNONYM_MAP.put("tutorial", Arrays.asList("guide", "howto", "walkthrough"));

        // Technical terms
        SYNONYM_MAP.put("api", Arrays.asList("interface", "endpoint", "service"));
        SYNONYM_MAP.put("framework", Arrays.asList("library", "toolkit", "platform"));
        SYNONYM_MAP.put("bug", Arrays.asList("error", "issue", "defect"));
    }

    /**
     * Expand query with synonyms.
     */
    public List<String> expandQuery(String query) {
        if (query == null || query.isEmpty()) {
            return List.of(query);
        }

        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.add(query); // Always include original

        // Tokenize query
        String[] tokens = query.toLowerCase().split("\\s+");

        // For each token, add synonyms
        for (String token : tokens) {
            List<String> synonyms = SYNONYM_MAP.get(token);
            if (synonyms != null) {
                expandedTerms.addAll(synonyms);
            }
        }

        return new ArrayList<>(expandedTerms);
    }

    /**
     * Expand query with weighted terms (for ranking).
     * Original query has highest weight.
     */
    public Map<String, Double> expandQueryWithWeights(String query) {
        Map<String, Double> weightedTerms = new HashMap<>();

        // Original query gets full weight
        weightedTerms.put(query, 1.0);

        String[] tokens = query.toLowerCase().split("\\s+");
        for (String token : tokens) {
            List<String> synonyms = SYNONYM_MAP.get(token);
            if (synonyms != null) {
                // Synonyms get 70% weight
                for (String synonym : synonyms) {
                    weightedTerms.put(synonym, 0.7);
                }
            }
        }

        return weightedTerms;
    }

    /**
     * Generate "Did you mean?" suggestions.
     */
    public List<String> generateSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();

        // Simple approach: check for common misspellings
        Map<String, String> commonMisspellings = new HashMap<>();
        commonMisspellings.put("algoritm", "algorithm");
        commonMisspellings.put("pyton", "python");
        commonMisspellings.put("javascirpt", "javascript");
        commonMisspellings.put("databse", "database");

        String lowerQuery = query.toLowerCase();
        for (Map.Entry<String, String> entry : commonMisspellings.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                suggestions.add(query.replace(entry.getKey(), entry.getValue()));
            }
        }

        return suggestions;
    }

    /**
     * Extract key terms from query (remove stop words).
     */
    public List<String> extractKeyTerms(String query) {
        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at",
                "to", "for", "of", "with", "by", "from", "is", "are", "was", "were");

        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(term -> !stopWords.contains(term))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toList());
    }

    /**
     * Add synonym to the dictionary.
     */
    public void addSynonym(String term, String synonym) {
        SYNONYM_MAP.computeIfAbsent(term.toLowerCase(), k -> new ArrayList<>())
                .add(synonym.toLowerCase());
    }

    /**
     * Get all synonyms for a term.
     */
    public List<String> getSynonyms(String term) {
        return SYNONYM_MAP.getOrDefault(term.toLowerCase(), List.of());
    }
}
