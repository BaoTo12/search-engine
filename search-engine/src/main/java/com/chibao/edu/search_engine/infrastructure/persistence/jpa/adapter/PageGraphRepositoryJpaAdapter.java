package com.chibao.edu.search_engine.infrastructure.persistence.jpa.adapter;

import com.chibao.edu.search_engine.domain.ranking.model.aggregate.PageGraph;
import com.chibao.edu.search_engine.domain.ranking.repository.PageGraphRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.PageGraphJpaEntity;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.entity.PageLinkJpaEntity;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository.PageGraphJpaRepository;
import com.chibao.edu.search_engine.infrastructure.persistence.jpa.repository.PageLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA adapter implementing PageGraphRepository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PageGraphRepositoryJpaAdapter implements PageGraphRepository {

    private final PageGraphJpaRepository pageGraphRepository;
    private final PageLinkJpaRepository pageLinkRepository;

    @Override
    @Transactional(readOnly = true)
    public PageGraph buildGraph() {
        log.info("Building page graph from database...");

        // Get all page entities
        List<PageGraphJpaEntity> pages = pageGraphRepository.findAll();

        if (pages.isEmpty()) {
            log.warn("No pages found in database");
            return PageGraph.empty();
        }

        // Create ID to URL mapping
        Map<Long, String> idToUrl = pages.stream()
                .collect(Collectors.toMap(PageGraphJpaEntity::getId, PageGraphJpaEntity::getUrl));

        // Get all links
        List<PageLinkJpaEntity> links = pageLinkRepository.findAll();

        // Build graph
        PageGraph graph = PageGraph.empty();

        for (PageLinkJpaEntity link : links) {
            String sourceUrl = idToUrl.get(link.getSourcePageId());
            String targetUrl = idToUrl.get(link.getTargetPageId());

            if (sourceUrl != null && targetUrl != null) {
                graph.addLink(sourceUrl, targetUrl);
            }
        }

        log.info("Built graph with {} pages and {} links", graph.size(), graph.getLinkCount());
        return graph;
    }

    @Override
    @Transactional
    public void savePageRankScores(Map<String, Double> scores) {
        log.info("Saving PageRank scores for {} pages", scores.size());

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String url = entry.getKey();
            Double score = entry.getValue();

            try {
                String urlHash = calculateHash(url);
                Optional<PageGraphJpaEntity> existing = pageGraphRepository.findByUrlHash(urlHash);

                if (existing.isPresent()) {
                    // Update existing
                    PageGraphJpaEntity entity = existing.get();
                    entity.setPreviousScore(entity.getPagerankScore());
                    entity.setPagerankScore(score);
                    entity.setLastCalculatedAt(LocalDateTime.now());
                    pageGraphRepository.save(entity);
                } else {
                    // Create new
                    PageGraphJpaEntity entity = PageGraphJpaEntity.builder()
                            .url(url)
                            .urlHash(urlHash)
                            .pagerankScore(score)
                            .previousScore(0.0)
                            .inboundLinksCount(0)
                            .outboundLinksCount(0)
                            .lastCalculatedAt(LocalDateTime.now())
                            .build();
                    pageGraphRepository.save(entity);
                }
            } catch (Exception e) {
                log.error("Error saving PageRank score for {}: {}", url, e.getMessage());
            }
        }

        log.info("Saved PageRank scores successfully");
    }

    @Override
    public Optional<Double> getPageRankScore(String url) {
        String urlHash = calculateHash(url);
        return pageGraphRepository.findByUrlHash(urlHash)
                .map(PageGraphJpaEntity::getPagerankScore);
    }

    @Override
    public Map<String, Double> getTopPages(int limit) {
        List<PageGraphJpaEntity> topPages = pageGraphRepository
                .findTopByPagerankScore(PageRequest.of(0, limit));

        return topPages.stream()
                .collect(Collectors.toMap(
                        PageGraphJpaEntity::getUrl,
                        PageGraphJpaEntity::getPagerankScore,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private String calculateHash(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
