package com.chibao.edu.search_engine.domain.ranking.service;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;
import com.chibao.edu.search_engine.domain.ranking.model.valueobject.PageRankScore;

import java.util.HashMap;
import java.util.Map;

public class PageRankCalculator {
    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    public void calculate(PageGraph graph) {
        int pageCount = graph.getPageCount();
        if (pageCount == 0)
            return;

        Map<String, Double> currentScores = initializeScores(graph, pageCount);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Map<String, Double> newScores = new HashMap<>();
            double delta = 0.0;

            for (String page : currentScores.keySet()) {
                double score = (1 - DAMPING_FACTOR) / pageCount;

                for (String incomingPage : graph.getIncomingLinks(page)) {
                    double incomingScore = currentScores.get(incomingPage);
                    int outgoingCount = graph.getOutgoingLinks(incomingPage).size();
                    if (outgoingCount > 0) {
                        score += DAMPING_FACTOR * (incomingScore / outgoingCount);
                    }
                }

                newScores.put(page, score);
                delta += Math.abs(score - currentScores.get(page));
            }

            currentScores = newScores;

            if (delta < CONVERGENCE_THRESHOLD) {
                break;
            }
        }

        for (Map.Entry<String, Double> entry : currentScores.entrySet()) {
            graph.setScore(entry.getKey(), PageRankScore.of(entry.getValue()));
        }
    }

    private Map<String, Double> initializeScores(PageGraph graph, int pageCount) {
        Map<String, Double> scores = new HashMap<>();
        double initialScore = 1.0 / pageCount;
        for (String url : graph.getAllScores().keySet()) {
            scores.put(url, initialScore);
        }
        return scores;
    }
}
