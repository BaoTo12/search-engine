package com.chibao.edu.search_engine.domain.ranking.repository;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;

public interface PageGraphRepository {
    PageGraph loadGraph();

    void saveScores(PageGraph graph);
}
