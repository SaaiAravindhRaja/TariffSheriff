package com.tariffsheriff.backend.news.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for TheNewsAPI integration
 * Fetches news articles based on search queries and filters
 */
@Component
public class TheNewsApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TheNewsApiClient.class);
    private static final int TIMEOUT_SECONDS = 30;
    
    private final WebClient webClient;
    private final String apiKey;
    
    @Value("${thenewsapi.candidate-limit:8}")
    private int candidateLimit;
    
    @Value("${thenewsapi.published-after:2020-01-01}")
    private String publishedAfter;
    
    public TheNewsApiClient(
            @Value("${thenewsapi.base-url}") String baseUrl,
            @Value("${thenewsapi.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    
    /**
     * Search for news articles with specified parameters
     * 
     * @param query Search query string
     * @param category Category filter (e.g., "business", null for all)
     * @param language Language filter (e.g., "en")
     * @param limit Maximum number of articles to return
     * @return List of news articles
     * @throws TheNewsApiException if API call fails
     */
    public List<NewsArticle> searchArticles(String query, String category, String language, int limit) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        logger.debug("Searching articles: query='{}', category='{}', language='{}', limit={}", 
                query, category, language, limit);
        long startTime = System.currentTimeMillis();
        
        try {
            // Build request URL with query parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/news/all")
                    .queryParam("api_token", apiKey)
                    .queryParam("search", query)
                    .queryParam("language", language != null ? language : "en")
                    .queryParam("limit", Math.min(limit, candidateLimit))
                    .queryParam("sort", "relevance_score")
                    .queryParam("published_after", publishedAfter);
            
            if (category != null && !category.trim().isEmpty()) {
                uriBuilder.queryParam("categories", category);
            }
            
            String uri = uriBuilder.build().toUriString();
            
            NewsApiResponse response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(NewsApiResponse.class)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .block();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (response == null || response.getData() == null) {
                logger.warn("Empty response from TheNewsAPI for query: '{}'", query);
                return new ArrayList<>();
            }
            
            List<NewsArticle> articles = response.getData();
            logger.info("Retrieved {} articles from TheNewsAPI in {}ms for query: '{}'", 
                    articles.size(), duration, query);
            
            // Filter articles with valid title and URL
            List<NewsArticle> validArticles = articles.stream()
                    .filter(article -> article.getTitle() != null && !article.getTitle().trim().isEmpty())
                    .filter(article -> article.getUrl() != null && !article.getUrl().trim().isEmpty())
                    .toList();
            
            if (validArticles.size() < articles.size()) {
                logger.debug("Filtered out {} articles with missing title or URL", 
                        articles.size() - validArticles.size());
            }
            
            return validArticles;
            
        } catch (WebClientResponseException e) {
            logger.error("TheNewsAPI error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Handle specific error cases
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new TheNewsApiException("Invalid API key or unauthorized access", e);
            } else if (e.getStatusCode().value() == 429) {
                throw new TheNewsApiException("Rate limit exceeded", e);
            } else if (e.getStatusCode().is5xxServerError()) {
                logger.warn("TheNewsAPI server error, returning empty results");
                return new ArrayList<>();
            }
            
            throw new TheNewsApiException("Failed to fetch articles: " + e.getMessage(), e);
            
        } catch (Exception e) {
            logger.error("Unexpected error calling TheNewsAPI", e);
            // Return empty list instead of failing completely
            logger.warn("Returning empty article list due to error");
            return new ArrayList<>();
        }
    }
    
    /**
     * News article data model
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsArticle {
        private String title;
        private String url;
        private String description;
        
        @JsonProperty("published_at")
        private String publishedAt;
        
        @JsonProperty("snippet")
        private String snippet;
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getPublishedAt() {
            return publishedAt;
        }
        
        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }
        
        public String getSnippet() {
            return snippet;
        }
        
        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }
    }
    
    /**
     * Response wrapper from TheNewsAPI
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NewsApiResponse {
        private List<NewsArticle> data;
        
        public List<NewsArticle> getData() {
            return data;
        }
        
        public void setData(List<NewsArticle> data) {
            this.data = data;
        }
    }
    
    /**
     * Exception thrown when TheNewsAPI calls fail
     */
    public static class TheNewsApiException extends RuntimeException {
        public TheNewsApiException(String message) {
            super(message);
        }
        
        public TheNewsApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
