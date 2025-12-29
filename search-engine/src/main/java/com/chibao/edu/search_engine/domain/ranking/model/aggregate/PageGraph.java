package com.chibao.edu.search_engine.domain.ranking.model.aggregate;

import com.chibao.edu.search_engine.domain.ranking.model.valueobject.PageRankScore;
import java.util.*;

public class PageGraph {
    private final Map<String, PageRankScore> scores;
    private final Map<String, List<String>> incomingLinks;
    private final Map<String, List<String>> outgoingLinks;

    public PageGraph() {
        this.scores = new HashMap<>();
        this.incomingLinks = new HashMap<>();
        this.outgoingLinks = new HashMap<>();
    }

    public void addLink(String fromUrl, String toUrl) {
        incomingLinks.computeIfAbsent(toUrl, k -> new ArrayList<>()).add(fromUrl);
        outgoingLinks.computeIfAbsent(fromUrl, k -> new ArrayList<>()).add(toUrl);
    }

    public void setScore(String url, PageRankScore score) {
        scores.put(url, score);
    }

    public PageRankScore getScore(String url) {
        return scores.getOrDefault(url, PageRankScore.zero());
    }

    public Map<String, PageRankScore> getAllScores() {
        return new HashMap<>(scores);
    }

    public List<String> getIncomingLinks(String url) {
        return incomingLinks.getOrDefault(url, List.of());
    }

    public List<String> getOutgoingLinks(String url) {
        return outgoingLinks.getOrDefault(url, List.of());
    }

    public int getPageCount() {
        return scores.size();
    }
}
