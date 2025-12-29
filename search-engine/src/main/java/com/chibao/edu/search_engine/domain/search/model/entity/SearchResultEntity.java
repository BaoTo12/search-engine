package com.chibao.edu.search_engine.domain.search.model.entity;

import com.chibao.edu.search_engine.domain.search.model.valueobject.SearchScore;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Entity representing a single search result.
 * Contains business logic related to search results.
 */
public class SearchResultEntity {
    private final String url;
    private final String title;
    private final String snippet;
    private final SearchScore score;
    private final LocalDateTime lastCrawled;

    private SearchResultEntity(
            String url,
            String title,
            String snippet,
            SearchScore score,
            LocalDateTime lastCrawled) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        this.url = url;
        this.title = title != null ? title : "";
        this.snippet = snippet != null ? snippet : "";
        this.score = score != null ? score : SearchScore.zero();
        this.lastCrawled = lastCrawled;
    }

    public static SearchResultEntity create(
            String url,
            String title,
            String snippet,
            double score,
            LocalDateTime lastCrawled) {
        return new SearchResultEntity(
                url,
                title,
                snippet,
                SearchScore.of(score),
                lastCrawled);
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public SearchScore getScore() {
        return score;
    }

    public LocalDateTime getLastCrawled() {
        return lastCrawled;
    }

    public boolean hasHigherScoreThan(SearchResultEntity other) {
        return this.score.isHigherThan(other.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SearchResultEntity))
            return false;
        SearchResultEntity that = (SearchResultEntity) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return String.format("SearchResult{url='%s', score=%s}", url, score);
    }
}
