package com.chibao.edu.search_engine.repository;

import com.chibao.edu.search_engine.entity.PageRankEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRankRepository extends JpaRepository<PageRankEntity, Long> {

    /**
     * Find PageRank by URL.
     */
    Optional<PageRankEntity> findByUrl(String url);

    /**
     * Get top N pages by PageRank score.
     */
    List<PageRankEntity> findTop100ByOrderByRankScoreDesc();

    /**
     * Get all PageRanks ordered by score.
     */
    List<PageRankEntity> findAllByOrderByRankScoreDesc();

    /**
     * Get average PageRank score.
     */
    @Query("SELECT AVG(pr.rankScore) FROM PageRankEntity pr")
    Double getAveragePageRank();
}
