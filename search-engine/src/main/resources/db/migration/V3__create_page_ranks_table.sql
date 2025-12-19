-- Migration: Create page_ranks table for storing PageRank scores
-- Version: V3__create_page_ranks_table.sql

CREATE TABLE IF NOT EXISTS page_ranks (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    rank_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    inbound_links INTEGER DEFAULT 0,
    outbound_links INTEGER DEFAULT 0,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rank_score ON page_ranks(rank_score DESC);
CREATE INDEX idx_url ON page_ranks(url);

COMMENT ON TABLE page_ranks IS 'Stores calculated PageRank scores for web pages';
COMMENT ON COLUMN page_ranks.url IS 'Unique URL of the web page';
COMMENT ON COLUMN page_ranks.rank_score IS 'Normalized PageRank score (0.0 - 1.0)';
COMMENT ON COLUMN page_ranks.inbound_links IS 'Number of pages linking to this page';
COMMENT ON COLUMN page_ranks.outbound_links IS 'Number of links from this page';
COMMENT ON COLUMN page_ranks.last_calculated IS 'Timestamp of last PageRank calculation';
