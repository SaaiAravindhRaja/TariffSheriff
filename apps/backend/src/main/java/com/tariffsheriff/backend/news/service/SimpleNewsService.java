package com.tariffsheriff.backend.news.service;

import com.tariffsheriff.backend.news.client.TheNewsApiClient;
import com.tariffsheriff.backend.news.dto.ArticleDto;
import com.tariffsheriff.backend.news.dto.NewsQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simplified news service without AI - just fetches and filters articles
 * Uses TheNewsAPI with keyword filtering for tariff-related content
 */
@Service("simpleNewsService")
public class SimpleNewsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleNewsService.class);
    
    private final TheNewsApiClient theNewsApiClient;
    
    @Value("${thenewsapi.candidate-limit:10}")
    private int candidateLimit;
    
    // In-memory cache for articles
    private List<ArticleDto> cachedArticles = new ArrayList<>();
    private long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour
    
    public SimpleNewsService(TheNewsApiClient theNewsApiClient) {
        this.theNewsApiClient = theNewsApiClient;
    }
    
    /**
     * Process a news query by fetching articles from TheNewsAPI
     * Filters for tariff-related content using keywords
     */
    public NewsQueryResponse processQuery(String query, String username, Long conversationId) {
        logger.info("Processing simple news query: {}", query);
        
        try {
            // Extract search terms - always include "tariff" to keep it relevant
            String searchQuery = extractSearchTerms(query);
            logger.debug("Search query: {}", searchQuery);
            
            // Fetch articles from TheNewsAPI
            List<TheNewsApiClient.NewsArticle> articles = theNewsApiClient.searchArticles(
                searchQuery, 
                "business", // Focus on business news
                "en",       // English only
                candidateLimit
            );
            logger.info("Fetched {} articles from TheNewsAPI", articles.size());
            
            // Filter articles that mention tariff-related keywords
            List<TheNewsApiClient.NewsArticle> filteredArticles = filterTariffArticles(articles);
            logger.info("Filtered to {} tariff-related articles", filteredArticles.size());
            
            // Convert to DTOs
            List<ArticleDto> articleDtos = filteredArticles.stream()
                .map(article -> {
                    ArticleDto dto = new ArticleDto();
                    dto.setTitle(article.getTitle());
                    dto.setUrl(article.getUrl());
                    dto.setContent(article.getDescription() != null ? article.getDescription() : "No description available");
                    dto.setQueryContext(null); // No AI context
                    dto.setSource("api");
                    dto.setPublishedAt(article.getPublishedAt());
                    dto.setImageUrl(article.getImageUrl());
                    return dto;
                })
                .collect(Collectors.toList());
            
            // Create simple summary
            String summary = createSimpleSummary(query, filteredArticles.size());
            
            return new NewsQueryResponse(
                summary,
                "api",
                articleDtos,
                conversationId
            );
            
        } catch (Exception e) {
            logger.error("Error processing simple news query", e);
            return new NewsQueryResponse(
                "Unable to fetch news articles. Please try again later.",
                "error",
                new ArrayList<>(),
                conversationId
            );
        }
    }
    
    /**
     * Extract search terms from user query
     * Always includes "tariff" to keep results relevant
     */
    private String extractSearchTerms(String query) {
        // Clean up the query
        String cleaned = query.toLowerCase().trim();
        
        // If query already mentions tariff, use as-is
        if (cleaned.contains("tariff") || cleaned.contains("duty") || cleaned.contains("trade")) {
            return query;
        }
        
        // Otherwise, add "tariff" to keep it relevant
        return query + " tariff";
    }
    
    /**
     * Filter articles to only include those mentioning tariff-related keywords
     */
    private List<TheNewsApiClient.NewsArticle> filterTariffArticles(List<TheNewsApiClient.NewsArticle> articles) {
        List<String> keywords = List.of(
            "tariff", "tariffs",
            "duty", "duties",
            "trade war", "trade policy",
            "import", "export",
            "customs", "trade agreement",
            "wto", "trade deal"
        );
        
        return articles.stream()
            .filter(article -> {
                String text = (article.getTitle() + " " + 
                              (article.getDescription() != null ? article.getDescription() : "")).toLowerCase();
                
                // Check if any keyword is present
                return keywords.stream().anyMatch(text::contains);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Create a simple summary without AI
     */
    private String createSimpleSummary(String query, int articleCount) {
        if (articleCount == 0) {
            return "No tariff-related articles found for your query. Try different search terms.";
        }
        
        return String.format("Found %d tariff-related article%s matching your search. " +
                           "Browse the articles below for the latest trade and tariff news.",
                           articleCount,
                           articleCount == 1 ? "" : "s");
    }
    
    /**
     * Get all articles with caching - fetches 12 articles once and caches for 1 hour
     * Pagination serves from cache (3 articles per page = 4 pages)
     */
    public List<ArticleDto> getAllArticles(int page, int limit) {
        logger.info("Fetching articles - page: {}, limit: {}", page, limit);
        
        // Check if cache is valid
        long now = System.currentTimeMillis();
        boolean cacheValid = !cachedArticles.isEmpty() && (now - cacheTimestamp) < CACHE_DURATION_MS;
        
        if (!cacheValid) {
            logger.info("Cache expired or empty, fetching fresh articles from API");
            cachedArticles = fetchAndCacheArticles();
            cacheTimestamp = now;
        } else {
            logger.info("Serving from cache ({} articles cached)", cachedArticles.size());
        }
        
        // Apply pagination to cached articles
        int start = page * limit;
        int end = Math.min(start + limit, cachedArticles.size());
        
        if (start >= cachedArticles.size()) {
            logger.info("Page {} is beyond available articles", page);
            return new ArrayList<>();
        }
        
        List<ArticleDto> paginatedArticles = cachedArticles.subList(start, end);
        logger.info("Returning {} articles for page {} from cache", paginatedArticles.size(), page);
        
        return paginatedArticles;
    }
    
    /**
     * Fetch 12 articles from API using 4 different queries (3 articles each)
     * This respects the free tier limit and provides variety
     */
    private List<ArticleDto> fetchAndCacheArticles() {
        try {
            // Use 4 diverse queries to get 12 articles total (3 per query)
            List<String> searchQueries = List.of(
                "tariff",
                "trade war",
                "import export",
                "customs duty"
            );
            
            List<TheNewsApiClient.NewsArticle> allFetchedArticles = new ArrayList<>();
            
            // Fetch articles with different queries
            for (String searchQuery : searchQueries) {
                try {
                    List<TheNewsApiClient.NewsArticle> articles = theNewsApiClient.searchArticles(
                        searchQuery, 
                        "business", 
                        "en",
                        candidateLimit
                    );
                    allFetchedArticles.addAll(articles);
                    logger.info("Fetched {} articles for query: '{}'", articles.size(), searchQuery);
                } catch (Exception e) {
                    logger.error("Failed to fetch articles for query '{}': {}", searchQuery, e.getMessage());
                    // Continue with other queries even if one fails
                }
            }
            
            logger.info("Fetched total {} articles from TheNewsAPI", allFetchedArticles.size());
            
            if (allFetchedArticles.isEmpty()) {
                logger.error("No articles fetched - API may have rate limit or connection issues");
                return new ArrayList<>();
            }
            
            // Remove duplicates by URL
            List<TheNewsApiClient.NewsArticle> uniqueArticles = allFetchedArticles.stream()
                .collect(Collectors.toMap(
                    TheNewsApiClient.NewsArticle::getUrl,
                    article -> article,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            
            logger.info("After deduplication: {} unique articles", uniqueArticles.size());
            
            // Filter for tariff-related content
            List<TheNewsApiClient.NewsArticle> filteredArticles = filterTariffArticles(uniqueArticles);
            logger.info("Filtered to {} tariff-related articles", filteredArticles.size());
            
            // Convert to DTOs and sort by published date (most recent first)
            List<ArticleDto> articles = filteredArticles.stream()
                .map(article -> {
                    ArticleDto dto = new ArticleDto();
                    dto.setTitle(article.getTitle());
                    dto.setUrl(article.getUrl());
                    dto.setContent(article.getDescription() != null ? article.getDescription() : "No description available");
                    dto.setQueryContext(null);
                    dto.setSource("api");
                    dto.setPublishedAt(article.getPublishedAt());
                    dto.setImageUrl(article.getImageUrl());
                    return dto;
                })
                .sorted((a, b) -> {
                    // Sort by published date, most recent first
                    if (a.getPublishedAt() == null) return 1;
                    if (b.getPublishedAt() == null) return -1;
                    return b.getPublishedAt().compareTo(a.getPublishedAt());
                })
                .collect(Collectors.toList());
            
            logger.info("Cached {} articles for future requests", articles.size());
            return articles;
                
        } catch (Exception e) {
            logger.error("Error fetching and caching articles", e);
            return new ArrayList<>();
        }
    }
}
