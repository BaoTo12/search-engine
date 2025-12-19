# Query Expansion & NLP Techniques

> **File:** [QueryExpansionService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/QueryExpansionService.java)  
> **Techniques:** Spell correction, synonyms, entity detection, intent classification

---

## Query Processing Pipeline

```
User Query: "java concurency tutoral"
    ↓
1. Spell Correction → "java concurrency tutorial"
    ↓
2. Synonym Expansion → ["multithreading", "parallel processing"]
    ↓
3. Entity Detection → ["Java" = programming language]
    ↓
4. Intent Classification → TUTORIAL
    ↓
Enhanced Elasticsearch Query
```

---

## 1. Spell Correction

### Levenshtein Distance (Edit Distance)

**Algorithm:** Minimum edits to transform string A → string B

```
Edits: insert, delete, replace

Example:
"concurency" → "concurrency"
- Replace 'u' with 'u' (0)
- Insert 'r' (1)
Total: 1 edit
```

### Dynamic Programming Implementation

```java
public class QueryExpansionService {
    
    public int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        // Initialize
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        // Fill DP table
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];  // No edit needed
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i-1][j],    // Delete
                                dp[i][j-1]),    // Insert
                        dp[i-1][j-1]            // Replace
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    public String correctSpelling(String word) {
        // Check against dictionary
        if (dictionary.contains(word)) {
            return word;
        }
        
        // Find closest match with edit distance ≤ 2
        String bestMatch = word;
        int minDistance = Integer.MAX_VALUE;
        
        for (String dictWord : dictionary) {
            // Optimization: skip if length diff > 2
            if (Math.abs(word.length() - dictWord.length()) > 2) {
                continue;
            }
            
            int distance = levenshteinDistance(word, dictWord);
            
            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                bestMatch = dictWord;
            }
        }
        
        return bestMatch;
    }
}
```

---

## 2. Synonym Expansion

### Synonym Database

```java
@Component
public class SynonymService {
    
    private final Map<String, Set<String>> synonyms = new HashMap<>();
    
    @PostConstruct
    public void loadSynonyms() {
        // Programming
        synonyms.put("java", Set.of("jdk", "jvm", "openjdk"));
        synonyms.put("concurrency", Set.of("multithreading", "parallel", "thread"));
        synonyms.put("tutorial", Set.of("guide", "howto", "documentation"));
        
        // Can load from database or external API
    }
    
    public Set<String> getSynonyms(String word) {
        return synonyms.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }
}
```

### Query Expansion

```java
public QueryExpansionResult expand(String query) {
    String[] words = query.toLowerCase().split("\\s+");
    Set<String> expandedTerms = new HashSet<>();
    
    for (String word : words) {
        // Original word
        expandedTerms.add(word);
        
        // Synonyms
        expandedTerms.addAll(synonymService.getSynonyms(word));
    }
    
    return QueryExpansionResult.builder()
        .originalQuery(query)
        .expandedTerms(expandedTerms)
        .build();
}
```

---

## 3. Entity Detection

### Named Entity Recognition (Simple)

```java
public class EntityDetector {
    
    private final Map<String, EntityType> entities = new HashMap<>();
    
    @PostConstruct
    public void loadEntities() {
        // Programming languages
        entities.put("java", EntityType.PROGRAMMING_LANGUAGE);
        entities.put("python", EntityType.PROGRAMMING_LANGUAGE);
        entities.put("javascript", EntityType.PROGRAMMING_LANGUAGE);
        
        // Frameworks
        entities.put("spring", EntityType.FRAMEWORK);
        entities.put("react", EntityType.FRAMEWORK);
        
        // Years
        for (int year = 2000; year <= 2025; year++) {
            entities.put(String.valueOf(year), EntityType.YEAR);
        }
    }
    
    public Map<String, EntityType> detectEntities(String query) {
        Map<String, EntityType> detected = new HashMap<>();
        String[] words = query.toLowerCase().split("\\s+");
        
        for (String word : words) {
            if (entities.containsKey(word)) {
                detected.put(word, entities.get(word));
            }
        }
        
        return detected;
    }
}
```

---

## 4. Intent Classification

### Rule-Based Intent Detection

```java
public enum QueryIntent {
    TUTORIAL,       // "how to", "tutorial", "guide"
    QUESTION,       // "what is", "why", "how"
    DEFINITION,     // "definition", "meaning"
    COMPARISON,     // "vs", "versus", "difference"
    TROUBLESHOOTING,// "error", "fix", "not working"
    GENERAL         // Default
}

public QueryIntent classifyIntent(String query) {
    String lower = query.toLowerCase();
    
    // Tutorial patterns
    if (lower.matches(".*(how to|tutorial|guide|learn).*")) {
        return QueryIntent.TUTORIAL;
    }
    
    // Question patterns
    if (lower.matches(".*(what is|why|how does|can).*")) {
        return QueryIntent.QUESTION;
    }
    
    // Definition
    if (lower.matches(".*(define|definition|meaning|what does .* mean).*")) {
        return QueryIntent.DEFINITION;
    }
    
    // Comparison
    if (lower.matches(".*(vs|versus|difference between|compare).*")) {
        return QueryIntent.COMPARISON;
    }
    
    // Troubleshooting
    if (lower.matches(".*(error|fix|not working|issue|problem).*")) {
        return QueryIntent.TROUBLESHOOTING;
    }
    
    return QueryIntent.GENERAL;
}
```

---

## Complete Query Expansion

```java
@Service
public class AdvancedSearchService {
    
    @Autowired
    private QueryExpansionService queryExpansionService;
    
    public SearchResponse search(String query, int page, int size) {
        // 1. Expand query
        QueryExpansionResult expansion = queryExpansionService.expand(query);
        
        // 2. Build Elasticsearch query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Original query (high boost)
        boolQuery.should(
            QueryBuilders.multiMatchQuery(expansion.getCorrectedQuery())
                .field("title", 3.0f)    // Title 3x weight
                .field("content", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
        );
        
        // Synonyms (medium boost)
        for (String synonym : expansion.getSynonyms()) {
            boolQuery.should(
                QueryBuilders.multiMatchQuery(synonym)
                    .field("title", 2.0f)
                    .field("content", 0.8f)
            );
        }
        
        // Intent-based filtering
        if (expansion.getIntent() == QueryIntent.TUTORIAL) {
            boolQuery.should(
                QueryBuilders.matchQuery("content", "tutorial guide how-to")
            );
        }
        
        // 3. Function score with PageRank
        FunctionScoreQueryBuilder functionScore = QueryBuilders.functionScoreQuery(
            boolQuery,
            ScoreFunctions.fieldValueFactorFunction("pageRank")
                .modifier(FieldValueFactorFunction.Modifier.LOG1P)
                .factor(2.0f)
        ).scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY);
        
        // 4. Execute search
        SearchRequest request = new SearchRequest("web_pages");
        request.source(
            new SearchSourceBuilder()
                .query(functionScore)
                .from(page * size)
                .size(size)
                .highlight(new HighlightBuilder()
                    .field("title")
                    .field("content")
                    .preTags("<mark>")
                    .postTags("</mark>")
                )
        );
        
        SearchResponse esResponse = elasticsearchClient.search(request, RequestOptions.DEFAULT);
        
        // 5. Build response
        List<SearchResult> results = parseResults(esResponse);
        
        return SearchResponse.builder()
            .results(results)
            .totalResults(esResponse.getHits().getTotalHits().value)
            .correctedQuery(expansion.getCorrectedQuery())
            .didYouMean(expansion.getDidYouMean())
            .relatedSearches(generateRelatedSearches(query))
            .build();
    }
}
```

---

## Result De-duplication

```java
public List<SearchResult> deduplicateResults(List<SearchResult> results) {
    Map<String, SearchResult> byDomain = new LinkedHashMap<>();
    int maxPerDomain = 2;
    
    for (SearchResult result : results) {
        String domain = extractDomain(result.getUrl());
        String key = domain + "_" + byDomain.values().stream()
            .filter(r -> extractDomain(r.getUrl()).equals(domain))
            .count();
        
        // Max 2 results per domain
        if (key.split("_")[1].length() < maxPerDomain) {
            byDomain.put(result.getUrl(), result);
        }
    }
    
    return new ArrayList<>(byDomain.values());
}
```

---

*Query expansion dramatically improves search relevance by understanding user intent!*
