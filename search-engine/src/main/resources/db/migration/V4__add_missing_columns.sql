-- Migration: Add missing columns to crawl_urls and domain_metadata
-- Version: V4__add_missing_columns.sql
-- Adds errorMessage and lastSuccessfulCrawl columns that were missing from V1

ALTER TABLE crawl_urls
ADD COLUMN IF NOT EXISTS error_message TEXT,
ADD COLUMN IF NOT EXISTS last_successful_crawl TIMESTAMP;

ALTER TABLE domain_metadata
ADD COLUMN IF NOT EXISTS total_failures INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN crawl_urls.error_message IS 'Error message from last failed crawl attempt';
COMMENT ON COLUMN crawl_urls.last_successful_crawl IS 'Timestamp of last successful crawl';
COMMENT ON COLUMN domain_metadata.total_failures IS 'Total number of failed crawl attempts for this domain';
