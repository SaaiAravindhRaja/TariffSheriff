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
     * Get all articles - fetches recent tariff news
     */
    public List<ArticleDto> getAllArticles() {
        logger.info("Fetching recent tariff news articles");
        
        try {
            // Fetch recent tariff-related articles
            List<TheNewsApiClient.NewsArticle> articles = theNewsApiClient.searchArticles(
                "tariff trade", 
                "business", 
                "en",
                20 // Get more articles for the main feed
            );
            logger.info("Fetched {} articles from TheNewsAPI", articles.size());
            
            // Filter for tariff-related content
            List<TheNewsApiClient.NewsArticle> filteredArticles = filterTariffArticles(articles);
            logger.info("Filtered to {} tariff-related articles", filteredArticles.size());
            
            // Convert to DTOs and sort by published date (most recent first)
            return filteredArticles.stream()
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
                
        } catch (Exception e) {
            logger.error("Error fetching articles", e);
            return new ArrayList<>();
        }
    }
}
