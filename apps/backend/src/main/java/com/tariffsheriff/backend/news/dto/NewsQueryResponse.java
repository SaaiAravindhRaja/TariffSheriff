package com.tariffsheriff.backend.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for news query processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing synthesized answer and relevant articles for a news query")
public class NewsQueryResponse {
    
    @Schema(
        description = "AI-generated synthesized answer summarizing the relevant articles",
        example = "Recent tariff changes have impacted trade between the US and China, with new duties imposed on technology goods. Experts suggest this may lead to supply chain disruptions.",
        required = true
    )
    private String synthesizedAnswer;
    
    @Schema(
        description = "Source of the articles - 'db' for cached results, 'api' for fresh fetch, 'error' for failures",
        example = "db",
        allowableValues = {"db", "api", "error"},
        required = true
    )
    private String source;
    
    @Schema(
        description = "List of relevant articles with content and context",
        required = true
    )
    private List<ArticleDto> articles;
    
    @Schema(
        description = "Conversation ID for threading multiple queries (null for first query)",
        example = "12345"
    )
    private Long conversationId;
}
