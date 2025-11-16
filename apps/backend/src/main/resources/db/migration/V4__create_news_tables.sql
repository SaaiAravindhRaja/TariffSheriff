-- Simple news feature - no AI, no embeddings, no conversation history
-- This migration creates placeholder tables for future enhancements
-- Currently the news feature fetches articles directly from TheNewsAPI

-- Placeholder for future article caching (optional)
CREATE TABLE IF NOT EXISTS news_cache (
    id BIGSERIAL PRIMARY KEY,
    query VARCHAR(500) NOT NULL,
    response_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_news_cache_query ON news_cache(query);
CREATE INDEX IF NOT EXISTS idx_news_cache_expires ON news_cache(expires_at);
