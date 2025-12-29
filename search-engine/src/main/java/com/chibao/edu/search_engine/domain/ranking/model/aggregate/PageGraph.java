package com.chibao.edu.search_engine.domain.ranking.model.aggregate;

import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * PageGraph aggregate root.
 * Represents the web page link graph for PageRank calculation.
 */
@Getter
@Builder
public class PageGraph {

    private final Set<String> pages;
    private final Map<String, Set<String>> adjacencyList; // page -> outbound links
    private final Map<String, Set<String>> reverseAdjacencyList; // page -> inbound links

    /**
     * Get all pages in the graph.
     */
    public Set<String> getPages() {
        return Collections.unmodifiableSet(pages);
    }

    /**
     * Get outbound links from a page.
     */
    public Set<String> getOutboundLinks(String page) {
        return adjacencyList.getOrDefault(page, Collections.emptySet());
    }

    /**
     * Get inbound links to a page.
     */
    public Set<String> getInboundLinks(String page) {
        return reverseAdjacencyList.getOrDefault(page, Collections.emptySet());
    }

    /**
     * Add a link from source to target.
     */
    public void addLink(String source, String target) {
        pages.add(source);
        pages.add(target);

        // Add to adjacency list
        adjacencyList.computeIfAbsent(source, k -> new HashSet<>()).add(target);

        // Add to reverse adjacency list
        reverseAdjacencyList.computeIfAbsent(target, k -> new HashSet<>()).add(source);
    }

    /**
     * Get number of pages in the graph.
     */
    public int size() {
        return pages.size();
    }

    /**
     * Get total number of links in the graph.
     */
    public int getLinkCount() {
        return adjacencyList.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * Create an empty graph.
     */
    public static PageGraph empty() {
        return PageGraph.builder()
                .pages(new HashSet<>())
                .adjacencyList(new HashMap<>())
                .reverseAdjacencyList(new HashMap<>())
                .build();
    }

    /**
     * Create a graph from links.
     */
    public static PageGraph fromLinks(List<PageLink> links) {
        PageGraph graph = empty();
        for (PageLink link : links) {
            graph.addLink(link.getSource(), link.getTarget());
        }
        return graph;
    }

    @Getter
    @Builder
    public static class PageLink {
        private String source;
        private String target;
        private String anchorText;
    }
}
