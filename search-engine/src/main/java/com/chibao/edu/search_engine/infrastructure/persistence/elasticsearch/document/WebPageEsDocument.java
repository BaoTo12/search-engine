package com.chibao.edu.search_engine.infrastructure.persistence.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch document for indexed web pages.
 */
@Document(indexName = "web-pages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPageEsDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String url;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String metaDescription;

    @Field(type = FieldType.Text, analyzer = "english")
    private String content;

    @Field(type = FieldType.Keyword)
    private List<String> tokens;

    @Field(type = FieldType.Keyword)
    private List<String> outboundLinks;

    @Field(type = FieldType.Keyword)
    private String language;

    @Field(type = FieldType.Keyword)
    private String contentHash;

    @Field(type = FieldType.Date)
    private LocalDateTime crawledAt;

    @Field(type = FieldType.Long)
    private Long contentSizeBytes;

    @Field(type = FieldType.Double)
    private Double pagerankScore;
}
