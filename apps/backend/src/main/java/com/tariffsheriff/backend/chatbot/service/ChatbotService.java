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
 * Core service for processing chat queries using LLM and tool orchestration
 */
@Service
public class ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    
    @Autowired
    public ChatbotService(LlmClient llmClient, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
    }
    
    /**
     * Main method to process user queries using two-phase LLM interaction
     */
    public ChatQueryResponse processQuery(ChatQueryRequest request) {
        long startTime = System.currentTimeMillis();
        String conversationId = request.getConversationId();
        
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }
        
        try {
            // Validate input
            validateQuery(request.getQuery());
            
            logger.info("Processing query for conversation {}: {}", conversationId, request.getQuery());
            
            // Phase 1: Query analysis and tool selection
            ToolCall toolCall = callLlmForToolSelection(request.getQuery());
            
            // Phase 2: Tool execution
            ToolResult toolResult = executeToolCall(toolCall);
            
            // Phase 3: Response generation
            String response = callLlmForResponse(request.getQuery(), toolResult);
            
            // Build response
            ChatQueryResponse chatResponse = new ChatQueryResponse(response, conversationId);
            chatResponse.setToolsUsed(List.of(toolCall.getName()));
            chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully processed query for conversation {} in {}ms", 
                    conversationId, chatResponse.getProcessingTimeMs());
            
            return chatResponse;
            
        } catch (ChatbotException e) {
            logger.warn("Chatbot error for conversation {}: {}", conversationId, e.getMessage());
            return createErrorResponse(conversationId, e.getMessage(), e.getSuggestion(), startTime);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing query for conversation {}", conversationId, e);
            return createErrorResponse(conversationId, 
                    "I'm having trouble processing your request right now.", 
                    "Please try again in a moment or rephrase your question.", 
                    startTime);
        }
    }
    
    /**
     * Phase 1: Call LLM for tool selection
     */
    private ToolCall callLlmForToolSelection(String query) {
        try {
            List<ToolDefinition> availableTools = toolRegistry.getAvailableTools();
            
            if (availableTools.isEmpty()) {
                throw new LlmServiceException("No tools are currently available", 
                        "Please try again later or contact support if the problem persists.");
            }
            
            logger.debug("Calling LLM for tool selection with {} available tools", availableTools.size());
            return llmClient.sendToolSelectionRequest(query, availableTools);
            
        } catch (LlmServiceException e) {
            throw e; // Re-throw LLM exceptions as-is
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
            throw e; // Re-throw tool exceptions as-is
        } catch (Exception e) {
            logger.error("Unexpected error executing tool {}", toolCall.getName(), e);
            throw new ToolExecutionException(toolCall.getName(), 
                    "Unexpected error during tool execution", e);
        }
    }
    
    /**
     * Phase 3: Call LLM for response generation
     */
    private String callLlmForResponse(String query, ToolResult toolResult) {
        try {
            logger.debug("Calling LLM for response generation");
            
            String toolData = toolResult.getData();
            if (toolData == null || toolData.trim().isEmpty()) {
                toolData = "No data was returned from the tool execution.";
            }
            
            return llmClient.sendResponseGenerationRequest(query, toolData);
            
        } catch (LlmServiceException e) {
            throw e; // Re-throw LLM exceptions as-is
        } catch (Exception e) {
            logger.error("Error in response generation phase", e);
            throw new LlmServiceException("Failed to generate a response", e);
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
        
        // Basic sanitization - remove potentially harmful content
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
        
        // Format error message with suggestion
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