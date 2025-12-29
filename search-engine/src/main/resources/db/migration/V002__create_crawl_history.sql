-- Create crawl_history table for tracking completed crawls
CREATE TABLE crawl_history (
    id BIGSERIAL PRIMARY KEY,
    crawl_url_id BIGINT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    
    -- Crawl result
    status_code INTEGER,
    content_type VARCHAR(100),
    content_size_bytes BIGINT,
    content_hash VARCHAR(64),
    
    -- Timestamps
    crawled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms INTEGER,
    
    -- Links extracted
    outbound_links_count INTEGER DEFAULT 0,
    
    -- Error information
    error_message TEXT,
    error_type VARCHAR(100),
    
    FOREIGN KEY (crawl_url_id) REFERENCES crawl_urls(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_crawl_history_url_id ON crawl_history(crawl_url_id);
CREATE INDEX idx_crawl_history_crawled_at ON crawl_history(crawled_at DESC);
CREATE INDEX idx_crawl_history_content_hash ON crawl_history(content_hash);
CREATE INDEX idx_crawl_history_status_code ON crawl_history(status_code);
