package com.tariffsheriff.backend.chatbot.controller;

import com.tariffsheriff.backend.chatbot.dto.ChatErrorResponse;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.exception.InvalidQueryException;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.exception.RateLimitExceededException;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import com.tariffsheriff.backend.chatbot.service.ChatbotService;
import com.tariffsheriff.backend.chatbot.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RateLimitService rateLimitService;

    /**
     * Process a natural language query and return AI-generated response
     * 
     * @param request The chat query request containing user's question
     * @param authentication The authenticated user context
     * @param httpRequest The HTTP request for logging
     * @return ChatQueryResponse with AI-generated answer or error details
     */
    @PostMapping("/query")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> processQuery(
            @Valid @RequestBody ChatQueryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        String clientIp = getClientIpAddress(httpRequest);
        
        log.info("Chat query received from user: {} (IP: {}) - Query: {}", 
                userEmail, clientIp, sanitizeForLogging(request.getQuery()));
        
        try {
            // Check rate limits first
            if (!rateLimitService.isAllowed(userEmail)) {
                RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(userEmail);
                throw new RateLimitExceededException(
                        status.getRequestsInLastMinute(),
                        status.getRequestsInLastHour(),
                        status.getMaxRequestsPerMinute(),
                        status.getMaxRequestsPerHour()
                );
            }
            
            // Set user ID in request for conversation tracking
            request.setUserId(userEmail);
            
            // Process the query through the chatbot service
            ChatQueryResponse response = chatbotService.processQuery(request);
            
            // Log successful processing
            log.info("Chat query processed successfully for user: {} - Conversation: {} - Processing time: {}ms", 
                    userEmail, response.getConversationId(), response.getProcessingTimeMs());
            
            return ResponseEntity.ok(response);
            
        } catch (RateLimitExceededException e) {
            log.warn("Rate limit exceeded for user: {} - Minute: {}/{}, Hour: {}/{}", 
                    userEmail, e.getRequestsInLastMinute(), e.getMaxRequestsPerMinute(),
                    e.getRequestsInLastHour(), e.getMaxRequestsPerHour());
            return handleChatbotException(e, request.getConversationId(), HttpStatus.TOO_MANY_REQUESTS);
            
        } catch (InvalidQueryException e) {
            log.warn("Invalid query from user: {} - Error: {}", userEmail, e.getMessage());
            return handleChatbotException(e, request.getConversationId(), HttpStatus.BAD_REQUEST);
            
        } catch (LlmServiceException e) {
            log.error("LLM service error for user: {} - Error: {}", userEmail, e.getMessage(), e);
            return handleChatbotException(e, request.getConversationId(), HttpStatus.SERVICE_UNAVAILABLE);
            
        } catch (ToolExecutionException e) {
            log.error("Tool execution error for user: {} - Tool: {} - Error: {}", 
                    userEmail, e.getToolName(), e.getMessage(), e);
            return handleChatbotException(e, request.getConversationId(), HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (ChatbotException e) {
            log.error("Chatbot error for user: {} - Error: {}", userEmail, e.getMessage(), e);
            return handleChatbotException(e, request.getConversationId(), HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            log.error("Unexpected error processing chat query for user: {}", userEmail, e);
            return handleUnexpectedError(request.getConversationId());
        }
    }

    /**
     * Get health status of the chatbot service
     * 
     * @return Health status and available tools count
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getHealth() {
        try {
            boolean isHealthy = chatbotService.isHealthy();
            int toolCount = chatbotService.getAvailableTools().size();
            int trackedUsers = rateLimitService.getTrackedUsersCount();
            
            if (isHealthy) {
                return ResponseEntity.ok(new HealthResponse(true, 
                        "Chatbot service is healthy", toolCount, trackedUsers));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new HealthResponse(false, 
                                "Chatbot service is not available", toolCount, trackedUsers));
            }
            
        } catch (Exception e) {
            log.error("Error checking chatbot health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthResponse(false, 
                            "Health check failed", 0, 0));
        }
    }

    /**
     * Get current rate limit status for the authenticated user
     * 
     * @param authentication The authenticated user context
     * @return Current rate limit usage and limits
     */
    @GetMapping("/rate-limit-status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RateLimitService.RateLimitStatus> getRateLimitStatus(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        
        try {
            RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(userEmail);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error getting rate limit status for user: {}", userEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get conversation history for the authenticated user
     * 
     * @param authentication The authenticated user context
     * @return List of conversation summaries
     */
    @GetMapping("/conversations")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getConversations(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        
        try {
            var conversations = chatbotService.getUserConversations(userEmail);
            return ResponseEntity.ok(conversations);
            
        } catch (Exception e) {
            log.error("Error getting conversations for user: {}", userEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific conversation details
     * 
     * @param conversationId The conversation ID
     * @param authentication The authenticated user context
     * @return Conversation details with message history
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        
        try {
            var conversation = chatbotService.getConversation(userEmail, conversationId);
            if (conversation == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(conversation);
            
        } catch (Exception e) {
            log.error("Error getting conversation {} for user: {}", conversationId, userEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a conversation
     * 
     * @param conversationId The conversation ID
     * @param authentication The authenticated user context
     * @return Success status
     */
    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        
        String userEmail = authentication != null ? authentication.getName() : "anonymous";
        
        try {
            boolean deleted = chatbotService.deleteConversation(userEmail, conversationId);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error deleting conversation {} for user: {}", conversationId, userEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        private int availableTools;
        private int trackedUsers;

        public HealthResponse(boolean healthy, String message, int availableTools, int trackedUsers) {
            this.healthy = healthy;
            this.message = message;
            this.availableTools = availableTools;
            this.trackedUsers = trackedUsers;
        }

        // Getters
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
        public int getAvailableTools() { return availableTools; }
        public int getTrackedUsers() { return trackedUsers; }
    }
}