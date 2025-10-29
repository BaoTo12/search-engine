package com.chibao.edu.search_engine.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.chibao.edu.search_engine.dto.search.SearchRequest;
import com.chibao.edu.search_engine.dto.search.SearchResponse;
import com.chibao.edu.search_engine.dto.search.SearchResult;
import com.chibao.edu.search_engine.entity.WebPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Senior Level: Advanced Query Processing with Expansion & Semantic Features
 * <p>
 * Features:
 * 1. Query expansion using synonyms
 * 2. Spelling correction (did you mean?)
 * 3. Query understanding (entity detection, intent classification)
 * 4. Result diversification
 * 5. Personalization hooks
 * 6. Click-through rate (CTR) boosting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchService basicSearchService;

    // Synonym mapping (in production, load from database or file)
    private final Map<String, List<String>> synonymMap = initSynonyms();

    // Common misspellings (in production, use phonetic algorithms or ML)
    private final Map<String, String> spellingCorrections = initSpellingCorrections();

    /**
     * Advanced search with query expansion and semantic understanding
     */
    public SearchResponse advancedSearch(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        String originalQuery = request.getQuery();
        log.info("Advanced search: '{}'", originalQuery);

        // Step 1: Query preprocessing
        String processedQuery = preprocessQuery(originalQuery);

        // Step 2: Spelling correction
        String correctedQuery = correctSpelling(processedQuery);
        boolean wasSpellingCorrected = !correctedQuery.equals(processedQuery);

        // Step 3: Query expansion with synonyms
        Set<String> expandedTerms = expandQuery(correctedQuery);

        // Step 4: Entity detection (optional advanced feature)
        List<String> detectedEntities = detectEntities(correctedQuery);

        // Step 5: Build advanced Elasticsearch query
        NativeQuery searchQuery = buildAdvancedQuery(
                correctedQuery,
                expandedTerms,
                detectedEntities,
                request.getPage(),
                request.getSize(),
                request.getSortBy()
        );

        // Step 6: Execute search
        SearchHits<WebPage> searchHits = elasticsearchOperations.search(
                searchQuery,
                WebPage.class
        );

        // Step 7: Diversify results (avoid too many from same domain)
        List<SearchResult> results = diversifyResults(searchHits);

        // Step 8: Apply personalization (if user context available)
        // results = personalizeResults(results, request.getUserContext());

        long executionTime = System.currentTimeMillis() - startTime;

        SearchResponse response = SearchResponse.builder()
                .query(originalQuery)
                .totalResults(searchHits.getTotalHits())
                .page(request.getPage())
                .size(request.getSize())
                .results(results)
                .executionTimeMs(executionTime)
                .build();

        // Add spelling suggestion if corrected
        if (wasSpellingCorrected) {
            log.info("Spelling corrected: '{}' -> '{}'", processedQuery, correctedQuery);
            // In production, add to response metadata
        }

        log.info("Advanced search completed in {}ms, found {} results",
                executionTime, searchHits.getTotalHits());

        return response;
    }

    /**
     * Preprocess query: lowercase, trim, remove special chars
     */
    private String preprocessQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ");
    }

    /**
     * Correct common spelling mistakes
     */
    private String correctSpelling(String query) {
        String[] words = query.split("\\s+");
        StringBuilder corrected = new StringBuilder();

        for (String word : words) {
            String correction = spellingCorrections.getOrDefault(word, word);
            corrected.append(correction).append(" ");
        }

        return corrected.toString().trim();
    }

    /**
     * Expand query with synonyms
     */
    private Set<String> expandQuery(String query) {
        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.add(query);

        String[] words = query.split("\\s+");

        for (String word : words) {
            // Add original word
            expandedTerms.add(word);

            // Add synonyms
            List<String> synonyms = synonymMap.get(word);
            if (synonyms != null) {
                expandedTerms.addAll(synonyms);
                log.debug("Expanded '{}' with synonyms: {}", word, synonyms);
            }
        }

        return expandedTerms;
    }

    /**
     * Detect named entities in query (simplified version)
     * In production, use NLP library like Stanford NER or spaCy
     */
    private List<String> detectEntities(String query) {
        List<String> entities = new ArrayList<>();

        // Detect programming languages
        List<String> programmingLangs = Arrays.asList(
                "java", "python", "javascript", "cpp", "csharp",
                "ruby", "go", "rust", "kotlin", "swift"
        );

        for (String lang : programmingLangs) {
            if (query.contains(lang)) {
                entities.add("programming:" + lang);
            }
        }

        // Detect years
        if (query.matches(".*\\b(19|20)\\d{2}\\b.*")) {
            entities.add("temporal:year");
        }

        // Detect question words (intent classification)
        if (query.matches("^(what|how|why|when|where|who)\\s.*")) {
            entities.add("intent:question");
        }

        return entities;
    }

    /**
     * Build advanced Elasticsearch query with boosting and expansion
     */
    private NativeQuery buildAdvancedQuery(
            String mainQuery,
            Set<String> expandedTerms,
            List<String> entities,
            int page,
            int size,
            String sortBy) {

        // Build multi‑match for the main query
        Query multiMatchMain = QueryBuilders.multiMatch(mm -> mm
                .query(mainQuery)
                .fields("title^5.0", "content^1.0", "tokens^2.0")
        );

        // Build multi‑match queries for expanded terms
        List<Query> expandedQueries = expandedTerms.stream()
                .filter(term -> !term.equals(mainQuery))
                .map(term -> QueryBuilders.multiMatch(mm -> mm
                        .query(term)
                        .fields("title^2.0", "content^0.5", "tokens^1.0")
                ))
                .toList();

        // Build match queries for entities
        List<Query> entityQueries = entities.stream()
                .filter(entity -> entity.startsWith("programming:"))
                .map(entity -> {
                    String lang = entity.substring("programming:".length());
                    return QueryBuilders.match(m -> m
                            .field("content")
                            .query(lang)
                            .boost(1.5f)
                    );
                })
                .toList();

        // Combine in a bool query
        Query boolQuery = QueryBuilders.bool(b -> {
            b.should(multiMatchMain);
            expandedQueries.forEach(b::should);
            entityQueries.forEach(b::should);
            b.minimumShouldMatch("1");
            return b;
        });

        // Wrap with function score to boost pageRank
        Query functionScoreQuery = QueryBuilders.functionScore(fs -> fs
                .query(q -> q.bool(bb -> bb.must(boolQuery)))
                .functions(fa -> fa.fieldValueFactor(fvf -> fvf
                        .field("pageRank")
                        .factor(0.5)
                ))
        );

        Pageable pageable = PageRequest.of(page, size);

        NativeQueryBuilder nqBuilder = new NativeQueryBuilder()
                .withQuery(functionScoreQuery)
                .withPageable(pageable);

        if ("date".equalsIgnoreCase(sortBy)) {
            nqBuilder.withSort(Sort.by(Sort.Direction.DESC, "lastCrawled"));
        } else if ("pagerank".equalsIgnoreCase(sortBy)) {
            nqBuilder.withSort(Sort.by(Sort.Direction.DESC, "pageRank"));
        }

        return nqBuilder.build();
    }

    /**
     * Diversify results to avoid showing too many from same domain
     */
    private List<SearchResult> diversifyResults(SearchHits<WebPage> searchHits) {
        List<SearchResult> results = new ArrayList<>();
        Map<String, Integer> domainCounts = new HashMap<>();

        int maxPerDomain = 3; // Maximum results from same domain in top results

        for (SearchHit<WebPage> hit : searchHits.getSearchHits()) {
            WebPage page = hit.getContent();
            String domain = page.getDomain();

            int count = domainCounts.getOrDefault(domain, 0);

            // Allow result if domain hasn't exceeded limit, or if we've exhausted other options
            if (count < maxPerDomain || results.size() < 10) {
                results.add(convertToSearchResult(hit));
                domainCounts.put(domain, count + 1);
            }
        }

        return results;
    }

    /**
     * Convert SearchHit to SearchResult
     */
    private SearchResult convertToSearchResult(SearchHit<WebPage> hit) {
        WebPage page = hit.getContent();

        return SearchResult.builder()
                .url(page.getUrl())
                .title(page.getTitle())
                .snippet(page.getSnippet())
                .score((double) hit.getScore())
                .lastCrawled(page.getLastCrawled())
                .build();
    }

    /**
     * Get search suggestions based on partial query
     */
    public List<String> getSuggestions(String prefix, int maxSuggestions) {
        if (prefix == null || prefix.length() < 2) {
            return Collections.emptyList();
        }

        try {
            // Search in titles for suggestions
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(QueryBuilders.matchPhrasePrefix(m -> m.field("title").query(prefix)))
                    .withPageable(PageRequest.of(0, maxSuggestions * 2))
                    .build();

            SearchHits<WebPage> hits = elasticsearchOperations.search(query, WebPage.class);

            // Extract unique title phrases
            return hits.getSearchHits().stream()
                    .map(hit -> hit.getContent().getTitle())
                    .distinct()
                    .limit(maxSuggestions)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting suggestions for: {}", prefix, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get related searches (queries that users searched after current query)
     */
    public List<String> getRelatedSearches(String query) {
        // In production, this would use query log analysis and ML
        // For now, return simple related terms based on synonyms

        Set<String> related = new HashSet<>();
        String[] words = query.split("\\s+");

        for (String word : words) {
            List<String> synonyms = synonymMap.get(word);
            if (synonyms != null && !synonyms.isEmpty()) {
                // Create related searches by substituting synonyms
                for (String synonym : synonyms) {
                    String relatedQuery = query.replace(word, synonym);
                    if (!relatedQuery.equals(query)) {
                        related.add(relatedQuery);
                    }
                }
            }
        }

        return related.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Initialize synonym map (in production, load from database)
     */
    private Map<String, List<String>> initSynonyms() {
        Map<String, List<String>> map = new HashMap<>();

        map.put("java", Arrays.asList("jvm", "jdk", "java programming"));
        map.put("python", Arrays.asList("py", "python programming"));
        map.put("javascript", Arrays.asList("js", "ecmascript", "node"));
        map.put("database", Arrays.asList("db", "data store", "storage"));
        map.put("algorithm", Arrays.asList("algo", "algorithm design"));
        map.put("tutorial", Arrays.asList("guide", "howto", "walkthrough"));
        map.put("error", Arrays.asList("exception", "bug", "issue", "problem"));
        map.put("performance", Arrays.asList("optimization", "speed", "efficiency"));
        map.put("security", Arrays.asList("auth", "authentication", "encryption"));

        return map;
    }

    /**
     * Initialize spelling corrections (in production, use ML or phonetic algorithms)
     */
    private Map<String, String> initSpellingCorrections() {
        Map<String, String> map = new HashMap<>();

        map.put("javascrpt", "javascript");
        map.put("pyhton", "python");
        map.put("algortihm", "algorithm");
        map.put("databse", "database");
        map.put("programing", "programming");
        map.put("tutorail", "tutorial");

        return map;
    }
}
