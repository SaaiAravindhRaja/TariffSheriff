-- Create pgvector extension if available
-- Note: pgvector provides optimized vector operations but is optional
-- Falls back to TEXT storage if extension is not available
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
    RAISE NOTICE 'pgvector extension enabled';
EXCEPTION
    WHEN insufficient_privilege OR undefined_file THEN
        RAISE NOTICE 'pgvector extension not available - using TEXT fallback for embeddings';
END
$$;

CREATE TABLE article_embeddings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    url TEXT NOT NULL UNIQUE,
    cleaned_text TEXT,
    -- Use TEXT as fallback if pgvector is not available
    -- Format: comma-separated float values
    embedding TEXT,
    topic VARCHAR(255),
    query_context TEXT,
    last_seen_query VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_embeddings_url ON article_embeddings(url);
CREATE INDEX idx_article_embeddings_topic ON article_embeddings(topic);


CREATE TABLE news_conversation_history (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    messages TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_conversation_history_username ON news_conversation_history(username);
CREATE INDEX idx_news_conversation_history_created_at ON news_conversation_history(created_at DESC);
