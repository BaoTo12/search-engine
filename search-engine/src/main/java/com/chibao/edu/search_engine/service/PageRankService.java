package com.chibao.edu.search_engine.service;

import com.chibao.edu.search_engine.entity.PageLink;
import com.chibao.edu.search_engine.entity.PageRankEntity;
import com.chibao.edu.search_engine.repository.PageLinkRepository;
import com.chibao.edu.search_engine.repository.PageRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PageRank Service - Implements Google's PageRank algorithm.
 * 
 * PageRank is a link analysis algorithm that measures the importance of web
 * pages
 * based on the number and quality of links pointing to them.
 * 
 * Formula:
 * PR(A) = (1-d) + d * Σ(PR(Ti) / C(Ti))
 * 
 * Where:
 * - PR(A) = PageRank of page A
 * - d = damping factor (typically 0.85)
 * - Ti = pages linking to A
 * - C(Ti) = number of outbound links from page Ti
 * 
 * Algorithm:
 * 1. Build link graph from crawled pages
 * 2. Initialize all pages with rank = 1.0 / N
 * 3. Iterate until convergence:
 * - For each page, calculate new rank based on inbound links
 * - Apply damping factor
 * 4. Store results in database
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PageRankService {

    private final PageLinkRepository pageLinkRepository;
    private final PageRankRepository pageRankRepository;

    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 20;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    /**
     * Calculate PageRank for all pages in the link graph.
     * This is a computationally expensive operation and should run periodically.
     *
     * @return Number of pages processed
     */
    @Transactional
    public int calculatePageRank() {
        log.info("Starting PageRank calculation...");
        long startTime = System.currentTimeMillis();

        // Step 1: Build link graph
        Map<String, List<String>> linkGraph = buildLinkGraph();
        if (linkGraph.isEmpty()) {
            log.warn("Link graph is empty, skipping PageRank calculation");
            return 0;
        }

        int totalPages = linkGraph.size();
        log.info("Built link graph with {} pages", totalPages);

        // Step 2: Initialize PageRank scores
        Map<String, Double> pageRank = initializePageRank(linkGraph);
        Map<String, Double> newPageRank = new HashMap<>();

        // Step 3: Iterate until convergence
        int iteration = 0;
        double maxDelta;

        do {
            maxDelta = 0.0;
            newPageRank.clear();

            for (String page : linkGraph.keySet()) {
                double rank = calculatePageRankForPage(page, pageRank, linkGraph);
                newPageRank.put(page, rank);

                // Track maximum change for convergence check
                double delta = Math.abs(rank - pageRank.get(page));
                maxDelta = Math.max(maxDelta, delta);
            }

            // Update page ranks
            pageRank.putAll(newPageRank);
            iteration++;

            log.debug("Iteration {}: maxDelta = {}", iteration, maxDelta);

        } while (iteration < MAX_ITERATIONS && maxDelta > CONVERGENCE_THRESHOLD);

        log.info("PageRank converged after {} iterations (maxDelta = {})", iteration, maxDelta);

        // Step 4: Normalize and store results
        normalizePageRanks(pageRank);
        storePageRanks(pageRank, linkGraph);

        long duration = System.currentTimeMillis() - startTime;
        log.info("PageRank calculation completed in {}s for {} pages", duration / 1000.0, totalPages);

        return totalPages;
    }

    /**
     * Build the link graph from PageLink entities.
     * 
     * @return Map of URL → List of outbound URLs
     */
    private Map<String, List<String>> buildLinkGraph() {
        Map<String, List<String>> graph = new HashMap<>();

        List<PageLink> allLinks = pageLinkRepository.findAll();

        for (PageLink link : allLinks) {
            String source = link.getSourceUrl();
            String target = link.getTargetUrl();

            // Add to graph
            graph.computeIfAbsent(source, k -> new ArrayList<>()).add(target);

            // Ensure target exists in graph (even with no outbound links)
            graph.putIfAbsent(target, new ArrayList<>());
        }

        return graph;
    }

    /**
     * Initialize PageRank scores (uniform distribution).
     */
    private Map<String, Double> initializePageRank(Map<String, List<String>> linkGraph) {
        Map<String, Double> pageRank = new HashMap<>();
        double initialRank = 1.0 / linkGraph.size();

        for (String page : linkGraph.keySet()) {
            pageRank.put(page, initialRank);
        }

        return pageRank;
    }

    /**
     * Calculate PageRank for a single page based on its inbound links.
     */
    private double calculatePageRankForPage(
            String page,
            Map<String, Double> currentRanks,
            Map<String, List<String>> linkGraph) {
        // Base rank (random surfer probability)
        double rank = (1.0 - DAMPING_FACTOR) / linkGraph.size();

        // Sum contributions from inbound links
        double inboundContribution = 0.0;

        for (Map.Entry<String, List<String>> entry : linkGraph.entrySet()) {
            String inboundPage = entry.getKey();
            List<String> outboundLinks = entry.getValue();

            // Check if inboundPage links to current page
            if (outboundLinks.contains(page)) {
                double inboundRank = currentRanks.get(inboundPage);
                int outboundCount = outboundLinks.size();

                if (outboundCount > 0) {
                    inboundContribution += inboundRank / outboundCount;
                }
            }
        }

        rank += DAMPING_FACTOR * inboundContribution;

        return rank;
    }

    /**
     * Normalize PageRank scores to sum to 1.0.
     */
    private void normalizePageRanks(Map<String, Double> pageRank) {
        double sum = pageRank.values().stream().mapToDouble(Double::doubleValue).sum();

        if (sum > 0) {
            for (String page : pageRank.keySet()) {
                pageRank.put(page, pageRank.get(page) / sum);
            }
        }
    }

    /**
     * Store PageRank scores in database.
     */
    private void storePageRanks(Map<String, Double> pageRanks, Map<String, List<String>> linkGraph) {
        log.info("Storing PageRank scores for {} pages", pageRanks.size());

        List<PageRankEntity> entities = new ArrayList<>();

        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            String url = entry.getKey();
            Double score = entry.getValue();

            // Count inbound and outbound links
            int outboundLinks = linkGraph.getOrDefault(url, Collections.emptyList()).size();
            long inboundLinks = pageLinkRepository.countByTargetUrl(url);

            PageRankEntity entity = PageRankEntity.builder()
                    .url(url)
                    .rankScore(score)
                    .inboundLinks((int) inboundLinks)
                    .outboundLinks(outboundLinks)
                    .build();

            entities.add(entity);
        }

        // Batch save
        pageRankRepository.saveAll(entities);
        log.info("Successfully stored {} PageRank scores", entities.size());
    }

    /**
     * Get PageRank score for a URL.
     */
    public double getPageRank(String url) {
        return pageRankRepository.findByUrl(url)
                .map(PageRankEntity::getRankScore)
                .orElse(0.0);
    }

    /**
     * Get top N pages by PageRank.
     */
    public List<PageRankEntity> getTopPages(int limit) {
        return pageRankRepository.findTop100ByOrderByRankScoreDesc()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Scheduled task to recalculate PageRank weekly.
     * Runs every Sunday at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void scheduledPageRankCalculation() {
        log.info("Starting scheduled PageRank calculation");
        try {
            calculatePageRank();
        } catch (Exception e) {
            log.error("Error during scheduled PageRank calculation", e);
        }
    }

    /**
     * Get PageRank statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPages", pageRankRepository.count());
        stats.put("averagePageRank", pageRankRepository.getAveragePageRank());
        stats.put("topPages", getTopPages(10));
        return stats;
    }
}
