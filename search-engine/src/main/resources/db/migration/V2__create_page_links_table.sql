-- Migration: Create page_links table for PageRank graph
-- Version: V2__create_page_links_table.sql

CREATE TABLE IF NOT EXISTS page_links (
    id BIGSERIAL PRIMARY KEY,
    source_url VARCHAR(2048) NOT NULL,
    target_url VARCHAR(2048) NOT NULL,
    anchor_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_link UNIQUE (source_url, target_url)
);

CREATE INDEX idx_source_url ON page_links(source_url);
CREATE INDEX idx_target_url ON page_links(target_url);

COMMENT ON TABLE page_links IS 'Stores link relationships between web pages for PageRank calculation';
COMMENT ON COLUMN page_links.source_url IS 'URL of the page containing the link';
COMMENT ON COLUMN page_links.target_url IS 'URL of the page being linked to';
COMMENT ON COLUMN page_links.anchor_text IS 'Text of the link (for relevance scoring)';
