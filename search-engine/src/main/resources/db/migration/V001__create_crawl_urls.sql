-- Create crawl_urls table for URL frontier management
CREATE TABLE crawl_urls (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    normalized_url VARCHAR(2048) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    url_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Crawl status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    depth INTEGER NOT NULL DEFAULT 0,
    max_depth INTEGER NOT NULL DEFAULT 3,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMP,
    crawled_at TIMESTAMP,
    next_crawl_at TIMESTAMP,
    
    -- Retry and error handling
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    
    -- Metadata
    source_url VARCHAR(2048),
    anchor_text TEXT,
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'BLOCKED'))
);

-- Indexes for performance
CREATE INDEX idx_crawl_urls_status ON crawl_urls(status);
CREATE INDEX idx_crawl_urls_domain ON crawl_urls(domain);
CREATE INDEX idx_crawl_urls_priority ON crawl_urls(priority DESC, created_at ASC);
CREATE INDEX idx_crawl_urls_scheduled_at ON crawl_urls(scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_crawl_urls_next_crawl ON crawl_urls(next_crawl_at) WHERE next_crawl_at IS NOT NULL;
CREATE INDEX idx_crawl_urls_url_hash ON crawl_urls(url_hash);

-- Composite index for scheduler query
CREATE INDEX idx_crawl_urls_scheduler ON crawl_urls(status, priority DESC, scheduled_at ASC);
