package com.tariffsheriff.backend.news.controller;

import com.tariffsheriff.backend.news.dto.ArticleDto;
import com.tariffsheriff.backend.news.dto.NewsQueryResponse;
import com.tariffsheriff.backend.news.service.SimpleNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for news-related endpoints
 * Handles news queries, article retrieval, and semantic search
 */
@RestController
@RequestMapping("/api/news")
@Slf4j
@Tag(name = "News", description = "Tariff news articles with keyword filtering")
public class NewsController {

    private final SimpleNewsService simpleNewsService;
    
    public NewsController(SimpleNewsService simpleNewsService) {
        this.simpleNewsService = simpleNewsService;
    }

    /**
     * Process a news query with keyword filtering
     * 
     * @param query The user's search query (required)
     * @return NewsQueryResponse with filtered tariff-related articles
     */
    @PostMapping("/query")
    @Operation(
        summary = "Search tariff news",
        description = "Search for tariff-related news articles using keyword filtering. " +
                     "Automatically filters for trade and tariff-related content."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Query processed successfully",
            content = @Content(schema = @Schema(implementation = NewsQueryResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameters"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during query processing"
        )
    })
    public ResponseEntity<NewsQueryResponse> processQuery(
            @Parameter(description = "Search query for tariff news", required = true)
            @RequestParam String query) {
        
        try {
            log.info("Received news query: '{}'", query);
            
            // Validate query
            if (query == null || query.trim().isEmpty()) {
                log.warn("Empty query received");
                return ResponseEntity.badRequest().build();
            }
            
            // Process query through simple news service
            NewsQueryResponse response = simpleNewsService.processQuery(query, null, null);
            
            log.info("Successfully processed query, returning {} articles", 
                response.getArticles().size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid query parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Error processing news query", e);
            
            // Return error response with fallback message
            NewsQueryResponse errorResponse = new NewsQueryResponse(
                "An error occurred while processing your query. Please try again.",
                "error",
                List.of(),
                null
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all stored articles
     * 
     * @return List of all articles in the database
     */
    @GetMapping("/articles")
    @Operation(
        summary = "Get all articles",
        description = "Retrieve all stored news articles from the database"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Articles retrieved successfully",
            content = @Content(schema = @Schema(implementation = ArticleDto.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public ResponseEntity<List<ArticleDto>> getAllArticles() {
        try {
            log.debug("Retrieving all articles");
            
            List<ArticleDto> articleDtos = simpleNewsService.getAllArticles();
            
            log.info("Retrieved {} articles", articleDtos.size());
            
            return ResponseEntity.ok(articleDtos);
            
        } catch (Exception e) {
            log.error("Error retrieving articles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // AI-powered search endpoints removed - using simple keyword filtering instead
}
