-- Migration: Initial schema for distributed web crawler
-- Version: V1__initial_schema.sql
-- Creates core tables for URL management and crawl metadata

-- Table: crawl_urls - URL crawl queue and status tracking
CREATE TABLE IF NOT EXISTS crawl_urls (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    url_hash VARCHAR(64) NOT NULL UNIQUE,
    domain VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    depth INTEGER NOT NULL DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 0,
    last_crawl_attempt TIMESTAMP,
    failure_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'BLOCKED', 'RATE_LIMITED'))
);

CREATE INDEX idx_crawl_urls_status ON crawl_urls(status);
CREATE INDEX idx_crawl_urls_domain ON crawl_urls(domain);
CREATE INDEX idx_crawl_urls_priority ON crawl_urls(priority DESC);
CREATE INDEX idx_crawl_urls_url_hash ON crawl_urls(url_hash);

COMMENT ON TABLE crawl_urls IS 'URL crawl queue with status tracking and prioritization';
COMMENT ON COLUMN crawl_urls.url_hash IS 'SHA-256 hash of normalized URL for fast deduplication';
COMMENT ON COLUMN crawl_urls.status IS 'Current crawl status: PENDING, IN_PROGRESS, COMPLETED, FAILED, BLOCKED, RATE_LIMITED';
COMMENT ON COLUMN crawl_urls.depth IS 'Crawl depth from seed URLs (0 = seed)';
COMMENT ON COLUMN crawl_urls.priority IS 'Crawl priority score (higher = more important)';

-- Table: domain_metadata - Per-domain configuration and statistics
CREATE TABLE IF NOT EXISTS domain_metadata (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    crawl_delay_ms INTEGER NOT NULL DEFAULT 1000,
    max_concurrent_requests INTEGER NOT NULL DEFAULT 5,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    total_pages_crawled BIGINT NOT NULL DEFAULT 0,
    last_crawl_time TIMESTAMP,
    robots_txt_content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_domain_metadata_domain ON domain_metadata(domain);
CREATE INDEX idx_domain_metadata_is_blocked ON domain_metadata(is_blocked);

COMMENT ON TABLE domain_metadata IS 'Per-domain crawling configuration and statistics';
COMMENT ON COLUMN domain_metadata.crawl_delay_ms IS 'Minimum delay between requests to this domain (milliseconds)';
COMMENT ON COLUMN domain_metadata.max_concurrent_requests IS 'Maximum concurrent requests allowed for this domain';
COMMENT ON COLUMN domain_metadata.robots_txt_content IS 'Cached robots.txt content for this domain';

-- Note: web_pages table is stored in Elasticsearch, not PostgreSQL
-- This is just for reference:
-- web_pages (Elasticsearch):
--   - url (keyword)
--   - title (text)
--   - content (text)
--   - domain (keyword)
--   - indexed_at (date)
--   - metadata (object)
