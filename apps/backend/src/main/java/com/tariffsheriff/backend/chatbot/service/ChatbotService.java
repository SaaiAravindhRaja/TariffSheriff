package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.exception.InvalidQueryException;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Simplified chatbot service for processing user queries using LLM and tools
 */
@Service
public class ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ConversationService conversationService;
    private final RateLimitService rateLimitService;
    
    @Autowired
    public ChatbotService(LlmClient llmClient, 
                         ToolRegistry toolRegistry,
                         ConversationService conversationService,
                         RateLimitService rateLimitService) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.conversationService = conversationService;
        this.rateLimitService = rateLimitService;
        
        logger.info("ChatbotService initialized with simplified architecture");
    }
    
    /**
     * Main method to process user queries with simple 3-phase flow:
     * 1. Understand: LLM selects tool or responds directly
     * 2. Execute: Tool fetches data
     * 3. Respond: LLM generates conversational response
     */
    public ChatQueryResponse processQuery(ChatQueryRequest request) {
        long startTime = System.currentTimeMillis();
        String conversationId = request.getConversationId();
        String userId = request.getUserId();
        
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }
        
        try {
            // Validate input
            validateQuery(request.getQuery());
            
            // Check rate limit
            if (userId != null && !rateLimitService.allowRequest(userId)) {
                throw new ChatbotException("Rate limit exceeded. Please try again later.");
            }
            
            logger.info("Processing query for conversation {}: {}", conversationId, request.getQuery());
            
            // Get conversation history for context
            List<ConversationService.ConversationMessage> conversationHistory = getConversationHistory(userId, conversationId);
            
            // Phase 1: Query analysis and tool selection
            ToolCall toolCall = callLlmForToolSelection(request.getQuery(), conversationHistory);
            
            // Handle direct response (no tool needed)
            if (ToolCall.DIRECT_RESPONSE_TOOL.equals(toolCall.getName())) {
                String directText = toolCall.getStringArgument("text", 
                        "I'm here to help! How else can I assist you today?");
                ChatQueryResponse chatResponse = new ChatQueryResponse(directText, conversationId);
                chatResponse.setToolsUsed(List.of(ToolCall.getDirectResponseToolLabel()));
                chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                
                if (userId != null) {
                    conversationService.storeMessage(userId, conversationId, request, chatResponse);
                }
                
                logger.info("Processed direct response query for conversation {} in {}ms", 
                        conversationId, chatResponse.getProcessingTimeMs());
                return chatResponse;
            }
            
            // Phase 2: Tool execution
            ToolResult toolResult = executeToolCall(toolCall);
            
            // Phase 3: Response generation
            String response = callLlmForResponse(request.getQuery(), toolResult, conversationHistory);
            
            // Build response
            ChatQueryResponse chatResponse = new ChatQueryResponse(response, conversationId);
            chatResponse.setToolsUsed(List.of(toolCall.getName()));
            chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            // Store in conversation history
            if (userId != null) {
                conversationService.storeMessage(userId, conversationId, request, chatResponse);
            }
            
            logger.info("Successfully processed query for conversation {} in {}ms", 
                    conversationId, chatResponse.getProcessingTimeMs());
            
            return chatResponse;
            
        } catch (InvalidQueryException e) {
            // Validation errors - guide the user
            logger.warn("Query validation failed for conversation {}: {}", conversationId, e.getMessage());
            return createErrorResponse(conversationId, 
                    e.getUserFriendlyMessage(), 
                    e.getSuggestion() != null ? e.getSuggestion() : "Please provide a clear question about tariffs, trade agreements, or HS codes.", 
                    startTime);
            
        } catch (LlmServiceException e) {
            // LLM service errors - explain AI is unavailable
            logger.error("LLM service error for conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse(conversationId, 
                    e.getUserFriendlyMessage(), 
                    e.getSuggestion(), 
                    startTime);
            
        } catch (ToolExecutionException e) {
            // Tool execution errors - explain what failed and how to proceed
            logger.error("Tool execution failed for conversation {} (tool: {}): {}", 
                    conversationId, e.getToolName(), e.getMessage(), e);
            return createErrorResponse(conversationId, 
                    e.getUserFriendlyMessage(), 
                    e.getSuggestion(), 
                    startTime);
            
        } catch (ChatbotException e) {
            // Generic chatbot errors
            logger.warn("Chatbot error for conversation {}: {}", conversationId, e.getMessage());
            return createErrorResponse(conversationId, 
                    e.getUserFriendlyMessage(), 
                    e.getSuggestion(), 
                    startTime);
            
        } catch (Exception e) {
            // Unexpected errors - log full details but show friendly message
            logger.error("Unexpected error processing query for conversation {}: {}", 
                    conversationId, e.getMessage(), e);
            return createErrorResponse(conversationId, 
                    "I'm having trouble processing your request right now.", 
                    "Please try again in a moment or rephrase your question. If the problem persists, contact support.", 
                    startTime);
        }
    }
    
    /**
     * Phase 1: Call LLM for tool selection
     */
    private ToolCall callLlmForToolSelection(String query, List<ConversationService.ConversationMessage> conversationHistory) {
        try {
            List<ToolDefinition> availableTools = toolRegistry.getAvailableTools();
            
            if (availableTools.isEmpty()) {
                throw new LlmServiceException("No tools are currently available", 
                        "Please try again later or contact support if the problem persists.");
            }
            
            logger.debug("Calling LLM for tool selection with {} available tools and {} history messages", 
                    availableTools.size(), conversationHistory.size());
            return llmClient.selectTool(query, availableTools, conversationHistory);
            
        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in tool selection phase", e);
            throw new LlmServiceException("Failed to analyze your query", e);
        }
    }
    
    /**
     * Phase 2: Execute the selected tool
     */
    private ToolResult executeToolCall(ToolCall toolCall) {
        try {
            logger.debug("Executing tool call: {}", toolCall.getName());
            
            if (!toolRegistry.isToolAvailable(toolCall.getName())) {
                throw new ToolExecutionException(toolCall.getName(), 
                        "The requested tool is not available");
            }
            
            ToolResult result = toolRegistry.executeToolCall(toolCall);
            
            if (!result.isSuccess()) {
                logger.warn("Tool execution failed: {}", result.getError());
                throw new ToolExecutionException(toolCall.getName(), 
                        result.getError() != null ? result.getError() : "Tool execution failed");
            }
            
            logger.debug("Tool {} executed successfully in {}ms", 
                    toolCall.getName(), result.getExecutionTimeMs());
            
            return result;
            
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error executing tool {}", toolCall.getName(), e);
            throw new ToolExecutionException(toolCall.getName(), 
                    "Unexpected error during tool execution", e);
        }
    }
    
    /**
     * Phase 3: Call LLM for response generation
     */
    private String callLlmForResponse(String query, ToolResult toolResult, List<ConversationService.ConversationMessage> conversationHistory) {
        try {
            logger.debug("Calling LLM for response generation with {} history messages", conversationHistory.size());
            
            String toolData = toolResult.getData();
            if (toolData == null || toolData.trim().isEmpty()) {
                toolData = "No data was returned from the tool execution.";
            }
            
            return llmClient.generateResponse(query, toolData, conversationHistory);
            
        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in response generation phase", e);
            throw new LlmServiceException("Failed to generate a response", e);
        }
    }
    
    /**
     * Get conversation history for context (last 5-10 messages)
     */
    private List<ConversationService.ConversationMessage> getConversationHistory(String userId, String conversationId) {
        if (userId == null || conversationId == null) {
            return List.of();
        }
        
        try {
            ConversationService.Conversation conversation = conversationService.getConversation(userId, conversationId);
            if (conversation == null) {
                return List.of();
            }
            
            List<ConversationService.ConversationMessage> allMessages = conversation.getMessages();
            
            // Get last 10 messages for context (5 exchanges)
            int startIndex = Math.max(0, allMessages.size() - 10);
            return allMessages.subList(startIndex, allMessages.size());
            
        } catch (Exception e) {
            logger.warn("Failed to retrieve conversation history for conversation {}", conversationId, e);
            return List.of();
        }
    }
    
    /**
     * Validate user query
     */
    private void validateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new InvalidQueryException("Query cannot be empty");
        }
        
        if (query.length() > 2000) {
            throw new InvalidQueryException("Query is too long. Please keep it under 2000 characters.");
        }
        
        String sanitized = query.trim();
        if (sanitized.length() < 3) {
            throw new InvalidQueryException("Query is too short. Please provide more details.");
        }
    }
    
    /**
     * Create error response
     */
    private ChatQueryResponse createErrorResponse(String conversationId, String message, 
                                                 String suggestion, long startTime) {
        ChatQueryResponse response = new ChatQueryResponse();
        response.setConversationId(conversationId);
        response.setSuccess(false);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        String fullMessage = message;
        if (StringUtils.hasText(suggestion)) {
            fullMessage += "\n\n" + suggestion;
        }
        response.setResponse(fullMessage);
        
        return response;
    }
    
    /**
     * Get available tools (for debugging/monitoring)
     */
    public List<ToolDefinition> getAvailableTools() {
        return toolRegistry.getAvailableTools();
    }
    
    /**
     * Check service health
     */
    public boolean isHealthy() {
        try {
            List<ToolDefinition> tools = toolRegistry.getAvailableTools();
            return !tools.isEmpty();
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }
}
