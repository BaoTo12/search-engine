package com.chibao.edu.search_engine.application.ranking.usecase;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;
import com.chibao.edu.search_engine.domain.ranking.repository.PageGraphRepository;
import com.chibao.edu.search_engine.domain.ranking.service.PageRankCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Use case for calculating PageRank scores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculatePageRankUseCase {

    private final PageGraphRepository pageGraphRepository;
    private final PageRankCalculator pageRankCalculator;

    public void execute() {
        log.info("Starting PageRank calculation...");

        try {
            // 1. Build the link graph from database
            log.info("Building page graph from crawled data...");
            PageGraph graph = pageGraphRepository.buildGraph();

            if (graph.size() == 0) {
                log.warn("No pages in graph. Skipping PageRank calculation.");
                return;
            }

            log.info("Graph built: {} pages, {} links", graph.size(), graph.getLinkCount());

            // 2. Calculate PageRank scores
            log.info("Calculating PageRank scores...");
            Map<String, Double> pageRanks = pageRankCalculator.calculate(graph);

            // 3. Normalize scores
            Map<String, Double> normalized = pageRankCalculator.normalize(pageRanks);

            // 4. Save scores to database
            log.info("Saving PageRank scores to database...");
            pageGraphRepository.savePageRankScores(normalized);

            // 5. Log top pages
            var topPages = pageRankCalculator.getTopPages(normalized, 10);
            log.info("Top 10 pages by PageRank:");
            topPages.forEach(entry -> log.info("  {} = {}", entry.getKey(), String.format("%.6f", entry.getValue())));

            log.info("PageRank calculation completed successfully!");

        } catch (Exception e) {
            log.error("Error calculating PageRank: {}", e.getMessage(), e);
            throw new RuntimeException("PageRank calculation failed", e);
        }
    }
}
