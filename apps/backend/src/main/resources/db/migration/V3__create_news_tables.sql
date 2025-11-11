CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE article_embeddings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    url TEXT NOT NULL UNIQUE,
    cleaned_text TEXT,
    embedding vector(1536),
    topic VARCHAR(255),
    query_context TEXT,
    last_seen_query VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_embeddings_url ON article_embeddings(url);
CREATE INDEX idx_article_embeddings_topic ON article_embeddings(topic);
CREATE INDEX idx_article_embeddings_embedding ON article_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);


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
