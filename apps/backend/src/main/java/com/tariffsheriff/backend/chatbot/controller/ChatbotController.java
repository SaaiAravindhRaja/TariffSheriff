package com.tariffsheriff.backend.chatbot.controller;

import com.tariffsheriff.backend.chatbot.dto.ChatConversationDetailDto;
import com.tariffsheriff.backend.chatbot.dto.ChatConversationSummaryDto;
import com.tariffsheriff.backend.chatbot.dto.ChatErrorResponse;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.exception.InvalidQueryException;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.service.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI chatbot functionality
 * Provides endpoints for natural language queries about trade data
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "http://127.0.0.1:8080"})
public class ChatbotController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatbotController.class);

    private final ChatbotService chatbotService;

    /**
     * Process a natural language query and return AI-generated response
     * 
     * @param request The chat query request containing user's question
     * @param authentication The authenticated user context
     * @param httpRequest The HTTP request for logging
     * @return ChatQueryResponse with AI-generated answer or error details
     */
    @PostMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> processQuery(
            @Valid @RequestBody ChatQueryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        String clientIp = getClientIpAddress(httpRequest);
        
        log.info("Chat query received from user: {} (IP: {}) - Query: {}", 
                userEmail, clientIp, sanitizeForLogging(request.getQuery()));
        
        try {
            ChatQueryResponse response = chatbotService.processQuery(request, userEmail);
            
            // Log successful processing
            log.info("Chat query processed successfully for user: {} - Conversation: {} - Processing time: {}ms", 
                    userEmail, response.getConversationId(), response.getProcessingTimeMs());
            
            return ResponseEntity.ok(response);
            
        } catch (InvalidQueryException e) {
            log.warn("Invalid query from user: {} - Error: {}", userEmail, e.getMessage());
            return handleChatbotException(e, request.getConversationId(), HttpStatus.BAD_REQUEST);
            
        } catch (LlmServiceException e) {
            log.error("LLM service error for user: {} - Error: {}", userEmail, e.getMessage(), e);
            return handleChatbotException(e, request.getConversationId(), HttpStatus.SERVICE_UNAVAILABLE);
            
        } catch (ChatbotException e) {
            log.error("Chatbot error for user: {} - Error: {}", userEmail, e.getMessage(), e);
            return handleChatbotException(e, request.getConversationId(), HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            log.error("Unexpected error processing chat query for user: {}", userEmail, e);
            return handleUnexpectedError(request.getConversationId());
        }
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listConversations(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        try {
            java.util.List<ChatConversationSummaryDto> summaries = chatbotService.listConversations(userEmail);
            return ResponseEntity.ok(summaries);
        } catch (ChatbotException e) {
            return handleChatbotException(e, null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error listing conversations for user {}", userEmail, e);
            return handleUnexpectedError(null);
        }
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        try {
            ChatConversationDetailDto detail = chatbotService.getConversationDetail(conversationId, userEmail);
            return ResponseEntity.ok(detail);
        } catch (ChatbotException e) {
            return handleChatbotException(e, conversationId, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error loading conversation {} for user {}", conversationId, userEmail, e);
            return handleUnexpectedError(conversationId);
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        try {
            chatbotService.deleteConversation(conversationId, userEmail);
            return ResponseEntity.noContent().build();
        } catch (ChatbotException e) {
            return handleChatbotException(e, conversationId, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error deleting conversation {} for user {}", conversationId, userEmail, e);
            return handleUnexpectedError(conversationId);
        }
    }

    /**
     * Get health status of the chatbot service
     * 
     * @return Health status and available tools count
     */
    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHealth() {
        try {
            boolean isHealthy = chatbotService.isHealthy();
            var capabilities = chatbotService.getCapabilities();
            HealthResponse body = new HealthResponse(
                    isHealthy,
                    isHealthy ? "Chatbot service is healthy" : "Chatbot service is not available",
                    capabilities
            );
            return isHealthy ? ResponseEntity.ok(body)
                    : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (Exception e) {
            log.error("Error checking chatbot health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthResponse(false, "Health check failed", java.util.List.of()));
        }
    }



    /**
     * Handle chatbot-specific exceptions
     */
    private ResponseEntity<ChatErrorResponse> handleChatbotException(
            ChatbotException e, String conversationId, HttpStatus status) {
        
        ChatErrorResponse errorResponse = new ChatErrorResponse();
        errorResponse.setError(e.getClass().getSimpleName());
        errorResponse.setMessage(e.getMessage());
        errorResponse.setSuggestion(e.getSuggestion());
        errorResponse.setConversationId(conversationId);
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle unexpected errors
     */
    private ResponseEntity<ChatErrorResponse> handleUnexpectedError(String conversationId) {
        ChatErrorResponse errorResponse = new ChatErrorResponse();
        errorResponse.setError("InternalServerError");
        errorResponse.setMessage("I'm having trouble processing your request right now.");
        errorResponse.setSuggestion("Please try again in a moment or rephrase your question.");
        errorResponse.setConversationId(conversationId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Sanitize query for logging (remove sensitive information)
     */
    private String sanitizeForLogging(String query) {
        if (query == null) return "null";
        
        // Truncate long queries for logging
        if (query.length() > 200) {
            return query.substring(0, 200) + "...";
        }
        
        return query;
    }

    /**
     * Health response DTO
     */
    public static class HealthResponse {
        private boolean healthy;
        private String message;
        private java.util.List<String> capabilities;

        public HealthResponse(boolean healthy, String message, java.util.List<String> capabilities) {
            this.healthy = healthy;
            this.message = message;
            this.capabilities = capabilities;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public java.util.List<String> getCapabilities() { return capabilities; }
    }
}
