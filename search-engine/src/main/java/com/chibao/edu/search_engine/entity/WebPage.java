package com.chibao.edu.search_engine.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "web_pages")
public class WebPage {

    @Id
    private String id; // URL hash

    @Field(type = FieldType.Keyword)
    private String url;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Text)
    private String snippet; // First 200 chars

    @Field(type = FieldType.Keyword)
    private List<String> outboundLinks;

    @Field(type = FieldType.Keyword)
    private Set<String> tokens;

    @Field(type = FieldType.Double)
    private Double pageRank;

    @Field(type = FieldType.Integer)
    private Integer inboundLinkCount;

    @Field(type = FieldType.Date)
    private LocalDateTime lastCrawled;

    @Field(type = FieldType.Date)
    private LocalDateTime lastIndexed;

    @Field(type = FieldType.Keyword)
    private String domain;

    @Field(type = FieldType.Integer)
    private Integer crawlDepth;

    @Field(type = FieldType.Keyword)
    private String contentType; // HTML, PDF, etc.

    @Field(type = FieldType.Long)
    private Long contentLength;
}
