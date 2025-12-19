package com.chibao.edu.search_engine.repository.elasticsearch;

import com.chibao.edu.search_engine.entity.WebPage;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebPageRepository extends ElasticsearchRepository<WebPage, String> {

    List<WebPage> findByDomain(String domain);

    @Query("{\"match\": {\"title\": \"?0\"}}")
    List<WebPage> findByTitleContaining(String title);

    @Query("{\"match\": {\"content\": \"?0\"}}")
    List<WebPage> findByContentContaining(String content);

    long countByDomain(String domain);
}
