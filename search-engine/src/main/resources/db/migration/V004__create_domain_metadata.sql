-- Create domain_metadata table for domain-level statistics and rate limiting
CREATE TABLE domain_metadata (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    
    -- Robots.txt information
    robots_txt_content TEXT,
    robots_txt_fetched_at TIMESTAMP,
    robots_txt_expires_at TIMESTAMP,
    crawl_delay_seconds INTEGER DEFAULT 1,
    disallowed_paths TEXT[], -- Array of disallowed path patterns
    
    -- Sitemap information
    sitemap_urls TEXT[], -- Array of sitemap URLs
    sitemap_fetched_at TIMESTAMP,
    
    -- Crawl statistics
    total_urls_discovered INTEGER DEFAULT 0,
    total_urls_crawled INTEGER DEFAULT 0,
    total_urls_failed INTEGER DEFAULT 0,
    average_response_time_ms INTEGER,
    
    -- Rate limiting
    last_crawl_at TIMESTAMP,
    requests_per_minute INTEGER DEFAULT 10,
    
    -- Domain quality metrics
    domain_authority_score DOUBLE PRECISION DEFAULT 0.0,
    average_content_quality DOUBLE PRECISION DEFAULT 0.0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_domain_metadata_domain ON domain_metadata(domain);
CREATE INDEX idx_domain_metadata_last_crawl ON domain_metadata(last_crawl_at);
CREATE INDEX idx_domain_metadata_authority ON domain_metadata(domain_authority_score DESC);
