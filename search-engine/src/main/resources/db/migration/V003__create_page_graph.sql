-- Create page_graph table for PageRank calculation
CREATE TABLE page_graph (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    url_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- PageRank scores
    pagerank_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    previous_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    
    -- Graph metrics
    inbound_links_count INTEGER NOT NULL DEFAULT 0,
    outbound_links_count INTEGER NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_calculated_at TIMESTAMP
);

-- Create page_links table for link graph
CREATE TABLE page_links (
    id BIGSERIAL PRIMARY KEY,
    source_page_id BIGINT NOT NULL,
    target_page_id BIGINT NOT NULL,
    anchor_text TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (source_page_id) REFERENCES page_graph(id) ON DELETE CASCADE,
    FOREIGN KEY (target_page_id) REFERENCES page_graph(id) ON DELETE CASCADE,
    
    UNIQUE (source_page_id, target_page_id)
);

-- Indexes for PageRank calculation
CREATE INDEX idx_page_graph_url_hash ON page_graph(url_hash);
CREATE INDEX idx_page_graph_pagerank ON page_graph(pagerank_score DESC);
CREATE INDEX idx_page_links_source ON page_links(source_page_id);
CREATE INDEX idx_page_links_target ON page_links(target_page_id);
