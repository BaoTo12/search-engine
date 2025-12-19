package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.dto.search.ExpandedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Query Expansion Service for enhancing search queries.
 * 
 * Features:
 * - Synonym expansion (e.g., "java" → "java programming", "JDK")
 * - Spell checking with Levenshtein distance
 * - Entity detection (programming languages, years, etc.)
 * - Intent classification (tutorial, documentation, question)
 * - "Did you mean?" suggestions
 * 
 * This improves search recall and user experience.
 */
@Service
@Slf4j
public class QueryExpansionService {

    // Synonym dictionary (in production, load from file or database)
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>() {
        {
            put("java", List.of("jdk", "jre", "java programming", "openjdk"));
            put("javascript", List.of("js", "ecmascript", "node", "nodejs"));
            put("python", List.of("py", "python programming", "python3"));
            put("algorithm", List.of("algo", "algorithms", "algorithmic"));
            put("database", List.of("db", "databases", "data store"));
            put("tutorial", List.of("guide", "how-to", "walkthrough", "introduction"));
            put("error", List.of("exception", "bug", "issue", "problem"));
            put("performance", List.of("speed", "optimization", "efficiency"));
        }
    };

    // Common programming languages for entity detection
    private static final Set<String> PROGRAMMING_LANGUAGES = Set.of(
            "java", "javascript", "python", "c++", "c#", "php", "ruby", "go",
            "rust", "kotlin", "swift", "typescript", "r", "scala", "perl");

    // Patterns for intent classification
    private static final Pattern QUESTION_PATTERN = Pattern.compile("\\b(how|what|why|when|where|who|which)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TUTORIAL_PATTERN = Pattern
            .compile("\\b(tutorial|guide|learn|introduction|getting started)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCUMENTATION_PATTERN = Pattern
            .compile("\\b(documentation|docs|api|reference|manual)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_PATTERN = Pattern.compile("\\b(error|exception|fix|solve|debug)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Expand a search query with synonyms, corrections, and metadata.
     *
     * @param originalQuery The user's original query
     * @return Expanded query object
     */
    public ExpandedQuery expandQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return ExpandedQuery.builder()
                    .original(originalQuery)
                    .corrected(originalQuery)
                    .synonyms(Collections.emptyList())
                    .entities(Collections.emptyList())
                    .intent("unknown")
                    .confidence(0.0)
                    .build();
        }

        String normalized = normalizeQuery(originalQuery);
        String corrected = spellCheck(normalized);
        List<String> synonyms = findSynonyms(normalized);
        List<String> entities = detectEntities(normalized);
        String intent = classifyIntent(normalized);
        double confidence = calculateConfidence(normalized, corrected, synonyms);

        return ExpandedQuery.builder()
                .original(originalQuery)
                .corrected(corrected)
                .synonyms(synonyms)
                .entities(entities)
                .intent(intent)
                .confidence(confidence)
                .build();
    }

    /**
     * Normalize query (lowercase, trim, remove special chars).
     */
    private String normalizeQuery(String query) {
        return query.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s+#-]", " ")
                .replaceAll("\\s+", " ");
    }

    /**
     * Find synonyms for query terms.
     */
    private List<String> findSynonyms(String query) {
        List<String> allSynonyms = new ArrayList<>();
        String[] terms = query.split("\\s+");

        for (String term : terms) {
            List<String> termSynonyms = SYNONYMS.get(term);
            if (termSynonyms != null) {
                allSynonyms.addAll(termSynonyms);
            }
        }

        return allSynonyms.stream()
                .distinct()
                .limit(10) // Limit to top 10 synonyms
                .collect(Collectors.toList());
    }

    /**
     * Simple spell checking using Levenshtein distance.
     * In production, use a proper spell-checking library.
     */
    private String spellCheck(String query) {
        String[] terms = query.split("\\s+");
        StringBuilder corrected = new StringBuilder();

        for (String term : terms) {
            String correction = findClosestWord(term);
            corrected.append(correction).append(" ");
        }

        return corrected.toString().trim();
    }

    /**
     * Find closest word using Levenshtein distance.
     * This is a simplified implementation - in production use a library.
     */
    private String findClosestWord(String term) {
        if (term.length() < 3) {
            return term; // Don't correct very short terms
        }

        // Check against known keywords
        Set<String> dictionary = new HashSet<>(SYNONYMS.keySet());
        dictionary.addAll(PROGRAMMING_LANGUAGES);

        String closest = term;
        int minDistance = Integer.MAX_VALUE;

        for (String word : dictionary) {
            int distance = levenshteinDistance(term, word);
            if (distance < minDistance && distance <= 2) { // Max 2 edits
                minDistance = distance;
                closest = word;
            }
        }

        return closest;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Detect entities in the query (programming languages, years, etc.).
     */
    private List<String> detectEntities(String query) {
        List<String> entities = new ArrayList<>();
        String[] terms = query.split("\\s+");

        for (String term : terms) {
            // Detect programming languages
            if (PROGRAMMING_LANGUAGES.contains(term)) {
                entities.add("language:" + term);
            }

            // Detect years (1900-2099)
            if (term.matches("(19|20)\\d{2}")) {
                entities.add("year:" + term);
            }

            // Detect versions (e.g., 3.0, 2.5.1)
            if (term.matches("\\d+(\\.\\d+)*")) {
                entities.add("version:" + term);
            }
        }

        return entities;
    }

    /**
     * Classify user intent based on query pattern.
     */
    private String classifyIntent(String query) {
        if (QUESTION_PATTERN.matcher(query).find()) {
            return "question";
        } else if (TUTORIAL_PATTERN.matcher(query).find()) {
            return "tutorial";
        } else if (DOCUMENTATION_PATTERN.matcher(query).find()) {
            return "documentation";
        } else if (ERROR_PATTERN.matcher(query).find()) {
            return "troubleshooting";
        } else {
            return "general";
        }
    }

    /**
     * Calculate confidence score for the expansion.
     */
    private double calculateConfidence(String original, String corrected, List<String> synonyms) {
        double confidence = 0.5; // Base confidence

        // Higher confidence if no corrections needed
        if (original.equals(corrected)) {
            confidence += 0.3;
        }

        // Higher confidence if synonyms found
        if (!synonyms.isEmpty()) {
            confidence += Math.min(0.2, synonyms.size() * 0.05);
        }

        return Math.min(1.0, confidence);
    }

    /**
     * Generate "Did you mean?" suggestion.
     */
    public String getDidYouMeanSuggestion(String originalQuery) {
        ExpandedQuery expanded = expandQuery(originalQuery);

        if (!expanded.getOriginal().equals(expanded.getCorrected())) {
            return expanded.getCorrected();
        }

        return null; // No suggestion needed
    }
}
