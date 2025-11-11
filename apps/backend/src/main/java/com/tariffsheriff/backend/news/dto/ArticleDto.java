package com.tariffsheriff.backend.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for article data in API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "News article with content and relevance context")
public class ArticleDto {
    
    @Schema(
        description = "Article title",
        example = "New Tariffs Announced on Steel Imports",
        required = true
    )
    private String title;
    
    @Schema(
        description = "URL to the original article",
        example = "https://example.com/news/tariffs-steel-2024",
        required = true
    )
    private String url;
    
    @Schema(
        description = "Cleaned article text content or preview (may be truncated to 500 characters)",
        example = "The government announced new tariffs on steel imports today, affecting trade relationships with multiple countries...",
        required = true
    )
    private String content;
    
    @Schema(
        description = "AI-generated context explaining how this article relates to the user's query",
        example = "This article provides context on recent tariff policy changes affecting international trade."
    )
    private String queryContext;
    
    @Schema(
        description = "Source of the article - 'db' for cached, 'api' for fresh fetch",
        example = "db",
        allowableValues = {"db", "api"}
    )
    private String source;
    
    @Schema(
        description = "Publication date in ISO format",
        example = "2024-01-15T10:30:00Z"
    )
    private String publishedAt;
    
    @Schema(
        description = "URL to article image/thumbnail",
        example = "https://example.com/images/article-thumbnail.jpg"
    )
    private String imageUrl;
}
