package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.ai.orchestration.AiOrchestrator;
import com.tariffsheriff.backend.ai.planning.ComplexityLevel;
import com.tariffsheriff.backend.ai.planning.QueryPlanner;
import com.tariffsheriff.backend.ai.planning.QueryAnalysis;
import com.tariffsheriff.backend.ai.planning.ExecutionPlan;
import com.tariffsheriff.backend.ai.context.ContextManager;
import com.tariffsheriff.backend.ai.context.QueryContext;
import com.tariffsheriff.backend.ai.context.UserContext;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Core service for processing chat queries using LLM and tool orchestration
 */
@Service
public class ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final FallbackService fallbackService;
    private final ChatCacheService cacheService;
    private final ConversationService conversationService;
    private final AiOrchestrator aiOrchestrator;
    private final QueryPlanner queryPlanner;
    private final ContextManager contextManager;
    
    // Progressive degradation components
    private final ToolHealthMonitor toolHealthMonitor;
    private final CircuitBreakerService circuitBreakerService;
    
    // Parallel processing components
    private final ExecutorService parallelExecutor;
    private final ExecutorService streamingExecutor;
    private final BlockingQueue<Runnable> batchQueue;
    private final AtomicInteger activeParallelTasks;
    
    // Service priority levels for resource allocation during failures
    private static final Map<String, Integer> SERVICE_PRIORITIES = Map.of(
            "tariff_lookup", 1,      // Highest priority
            "hs_code_finder", 2,
            "agreement_tool", 3,
            "compliance_analysis", 4,
            "market_intelligence", 5,
            "risk_assessment", 6     // Lowest priority
    );
    
    // Parallel processing configuration
    private static final int MAX_PARALLEL_TOOLS = 5;
    private static final int STREAMING_THREAD_POOL_SIZE = 3;
    private static final int BATCH_QUEUE_SIZE = 100;
    
    @Autowired
    public ChatbotService(LlmClient llmClient, ToolRegistry toolRegistry, FallbackService fallbackService,
                         ChatCacheService cacheService, ConversationService conversationService,
                         AiOrchestrator aiOrchestrator, QueryPlanner queryPlanner, ContextManager contextManager,
                         ToolHealthMonitor toolHealthMonitor, CircuitBreakerService circuitBreakerService) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.fallbackService = fallbackService;
        this.cacheService = cacheService;
        this.conversationService = conversationService;
        this.aiOrchestrator = aiOrchestrator;
        this.queryPlanner = queryPlanner;
        this.contextManager = contextManager;
        this.toolHealthMonitor = toolHealthMonitor;
        this.circuitBreakerService = circuitBreakerService;
        
        // Initialize parallel processing components
        this.parallelExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_TOOLS);
        this.streamingExecutor = Executors.newFixedThreadPool(STREAMING_THREAD_POOL_SIZE);
        this.batchQueue = new LinkedBlockingQueue<>(BATCH_QUEUE_SIZE);
        this.activeParallelTasks = new AtomicInteger(0);
        
        logger.info("ChatbotService initialized with parallel processing support");
    }
    
    /**
     * Main method to process user queries with enhanced AI orchestration
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
            
            logger.info("Processing query for conversation {}: {}", conversationId, request.getQuery());
            
            // Check cache first
            ChatQueryResponse cachedResponse = cacheService.getCachedResponse(request.getQuery());
            if (cachedResponse != null) {
                // Update conversation ID and processing time for cached response
                ChatQueryResponse response = new ChatQueryResponse(cachedResponse.getResponse(), conversationId);
                response.setToolsUsed(cachedResponse.getToolsUsed());
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                response.setCached(true);
                
                // Store in conversation history
                if (userId != null) {
                    conversationService.storeMessage(userId, conversationId, request, response);
                }
                
                logger.info("Returned cached response for conversation {} in {}ms", 
                        conversationId, response.getProcessingTimeMs());
                
                return response;
            }
            
            // Determine if query requires enhanced AI processing
            if (requiresEnhancedProcessing(request)) {
                logger.debug("Using enhanced AI orchestration for complex query");
                return aiOrchestrator.processComplexQuery(request);
            } else {
                logger.debug("Using standard processing for simple query");
                return processStandardQuery(request, startTime);
            }
            
        } catch (LlmServiceException e) {
            logger.warn("LLM service error for conversation {}: {}", conversationId, e.getMessage());
            
            // Check if this is a help query that can be handled by fallback
            if (fallbackService.isHelpQuery(request.getQuery())) {
                return fallbackService.generateFallbackResponse(request.getQuery(), conversationId, startTime);
            }
            
            // For LLM service errors, try fallback response
            logger.info("Attempting fallback response for conversation {}", conversationId);
            return fallbackService.generateFallbackResponse(request.getQuery(), conversationId, startTime);
            
        } catch (ChatbotException e) {
            logger.warn("Chatbot error for conversation {}: {}", conversationId, e.getMessage());
            return createErrorResponse(conversationId, e.getMessage(), e.getSuggestion(), startTime);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing query for conversation {}", conversationId, e);
            
            // For unexpected errors, also try fallback if it seems like a reasonable query
            if (request.getQuery() != null && request.getQuery().trim().length() > 3) {
                logger.info("Attempting fallback response for unexpected error in conversation {}", conversationId);
                return fallbackService.generateFallbackResponse(request.getQuery(), conversationId, startTime);
            }
            
            return createErrorResponse(conversationId, 
                    "I'm having trouble processing your request right now.", 
                    "Please try again in a moment or rephrase your question.", 
                    startTime);
        }
    }
    
    /**
     * Determine if query requires enhanced AI processing with detailed complexity assessment
     */
    private boolean requiresEnhancedProcessing(ChatQueryRequest request) {
        try {
            // Load user context for analysis
            UserContext userContext = contextManager.loadUserContext(request.getUserId());
            QueryContext queryContext = contextManager.buildQueryContext(request, userContext);
            
            // Perform comprehensive query analysis
            QueryAnalysis analysis = assessQueryComplexity(request.getQuery(), queryContext);
            
            // Log complexity assessment for monitoring
            logger.debug("Query complexity assessment for '{}': complexity={}, multiAgent={}, entities={}", 
                    request.getQuery().substring(0, Math.min(50, request.getQuery().length())),
                    analysis.getComplexity(), 
                    analysis.requiresMultiAgent(),
                    analysis.getExtractedEntities().size());
            
            // Use enhanced processing for medium/high complexity, multi-agent queries, or contextual references
            return analysis.getComplexity() != ComplexityLevel.LOW || 
                   analysis.requiresMultiAgent() ||
                   analysis.hasContextualReferences() ||
                   analysis.getExtractedEntities().size() > 3;
                   
        } catch (Exception e) {
            logger.warn("Error determining processing type, defaulting to standard", e);
            return false;
        }
    }
    
    /**
     * Assess query complexity with detailed analysis
     */
    private QueryAnalysis assessQueryComplexity(String query, QueryContext context) {
        try {
            return queryPlanner.analyzeQuery(query, context);
        } catch (Exception e) {
            logger.warn("Error in query analysis, using fallback assessment", e);
            
            // Fallback complexity assessment based on simple heuristics
            ComplexityLevel complexity = ComplexityLevel.LOW;
            boolean multiAgent = false;
            
            String lowerQuery = query.toLowerCase();
            
            // Check for complexity indicators
            if (lowerQuery.contains("compare") || lowerQuery.contains("vs") || lowerQuery.contains("versus")) {
                complexity = ComplexityLevel.MEDIUM;
                multiAgent = true;
            }
            
            if (lowerQuery.contains("analyze") || lowerQuery.contains("calculate") || lowerQuery.contains("optimize")) {
                complexity = ComplexityLevel.MEDIUM;
            }
            
            if (lowerQuery.contains("scenario") || lowerQuery.contains("what if") || lowerQuery.contains("multiple")) {
                complexity = ComplexityLevel.HIGH;
                multiAgent = true;
            }
            
            // Create basic analysis result
            return new QueryAnalysis(query, complexity, multiAgent, new ArrayList<>(), new ArrayList<>());
        }
    }
    
    /**
     * Process query using standard (original) approach
     */
    private ChatQueryResponse processStandardQuery(ChatQueryRequest request, long startTime) {
        String conversationId = request.getConversationId();
        String userId = request.getUserId();
        
        try {
            // Phase 1: Query analysis and tool selection
            ToolCall toolCall = callLlmForToolSelection(request.getQuery());
            if (ToolCall.DIRECT_RESPONSE_TOOL.equals(toolCall.getName())) {
                String directText = toolCall.getStringArgument("text", "I'm here to help! How else can I assist you today?");
                ChatQueryResponse chatResponse = new ChatQueryResponse(directText, conversationId);
                chatResponse.setToolsUsed(List.of(ToolCall.getDirectResponseToolLabel()));
                chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                cacheService.cacheResponse(request.getQuery(), chatResponse);
                if (userId != null) {
                    conversationService.storeMessage(userId, conversationId, request, chatResponse);
                }
                logger.info("Processed direct response query for conversation {} in {}ms", conversationId, chatResponse.getProcessingTimeMs());
                return chatResponse;
            }
            
            // Phase 2: Tool execution
            ToolResult toolResult = executeToolCall(toolCall);
            
            // Phase 3: Response generation
            String response = callLlmForResponse(request.getQuery(), toolResult);
            
            // Build response
            ChatQueryResponse chatResponse = new ChatQueryResponse(response, conversationId);
            chatResponse.setToolsUsed(List.of(toolCall.getName()));
            chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            // Cache the response
            cacheService.cacheResponse(request.getQuery(), chatResponse);
            
            // Store in conversation history
            if (userId != null) {
                conversationService.storeMessage(userId, conversationId, request, chatResponse);
            }
            
            logger.info("Successfully processed standard query for conversation {} in {}ms", 
                    conversationId, chatResponse.getProcessingTimeMs());
            
            return chatResponse;
            
        } catch (LlmServiceException e) {
            logger.warn("LLM service error for conversation {}: {}", conversationId, e.getMessage());
            
            // Check if this is a help query that can be handled by fallback
            if (fallbackService.isHelpQuery(request.getQuery())) {
                return fallbackService.generateFallbackResponse(request.getQuery(), conversationId, startTime);
            }
            
            // For LLM service errors, try fallback response
            logger.info("Attempting fallback response for conversation {}", conversationId);
            return fallbackService.generateFallbackResponse(request.getQuery(), conversationId, startTime);
            
        } catch (ChatbotException e) {
            logger.warn("Chatbot error for conversation {}: {}", conversationId, e.getMessage());
            return createErrorResponse(conversationId, e.getMessage(), e.getSuggestion(), startTime);
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
    
    /**
     * Get conversation history for a user
     */
    public List<ConversationService.ConversationSummary> getUserConversations(String userId) {
        return conversationService.getUserConversations(userId);
    }
    
    /**
     * Get specific conversation
     */
    public ConversationService.Conversation getConversation(String userId, String conversationId) {
        return conversationService.getConversation(userId, conversationId);
    }
    
    /**
     * Delete a conversation
     */
    public boolean deleteConversation(String userId, String conversationId) {
        return conversationService.deleteConversation(userId, conversationId);
    }
    
    /**
     * Clear all conversations for a user
     */
    public void clearUserConversations(String userId) {
        conversationService.clearUserConversations(userId);
    }
    
    /**
     * Synthesize results from multiple tool executions with intelligent combination
     */
    public String synthesizeMultipleResults(List<ToolResult> toolResults, String originalQuery, QueryContext context) {
        try {
            if (toolResults.isEmpty()) {
                return "I wasn't able to gather information to answer your question.";
            }
            
            // Filter successful results
            List<ToolResult> successfulResults = toolResults.stream()
                    .filter(ToolResult::isSuccess)
                    .collect(Collectors.toList());
            
            if (successfulResults.isEmpty()) {
                return handleAllToolFailures(toolResults, originalQuery);
            }
            
            // If only one successful result, return it directly
            if (successfulResults.size() == 1) {
                return successfulResults.get(0).getData();
            }
            
            // Combine multiple results intelligently
            return combineToolResults(successfulResults, originalQuery, context);
            
        } catch (Exception e) {
            logger.error("Error synthesizing multiple results", e);
            return "I encountered an issue combining the information. Please try rephrasing your question.";
        }
    }
    
    /**
     * Combine multiple tool results into coherent response
     */
    private String combineToolResults(List<ToolResult> results, String originalQuery, QueryContext context) {
        StringBuilder combinedResponse = new StringBuilder();
        Map<String, String> resultsByTool = new HashMap<>();
        
        // Organize results by tool type
        for (ToolResult result : results) {
            if (result.getData() != null && !result.getData().trim().isEmpty()) {
                resultsByTool.put(result.getToolName(), result.getData());
            }
        }
        
        // Determine combination strategy based on query type
        if (originalQuery.toLowerCase().contains("compare") || originalQuery.toLowerCase().contains("vs")) {
            return createComparisonResponse(resultsByTool, originalQuery);
        } else if (originalQuery.toLowerCase().contains("analyze") || originalQuery.toLowerCase().contains("assessment")) {
            return createAnalysisResponse(resultsByTool, originalQuery);
        } else {
            return createGeneralCombinedResponse(resultsByTool, originalQuery);
        }
    }
    
    /**
     * Create comparison-focused response
     */
    private String createComparisonResponse(Map<String, String> resultsByTool, String originalQuery) {
        StringBuilder response = new StringBuilder();
        response.append("Based on your comparison request, here's what I found:\n\n");
        
        int index = 1;
        for (Map.Entry<String, String> entry : resultsByTool.entrySet()) {
            response.append("**").append(index).append(". ").append(entry.getKey()).append(" Analysis:**\n");
            response.append(entry.getValue()).append("\n\n");
            index++;
        }
        
        response.append("**Summary:** ");
        response.append("The analysis above provides different perspectives on your query. ");
        response.append("Consider the trade-offs and specific requirements for your situation.");
        
        return response.toString();
    }
    
    /**
     * Create analysis-focused response
     */
    private String createAnalysisResponse(Map<String, String> resultsByTool, String originalQuery) {
        StringBuilder response = new StringBuilder();
        response.append("Here's my comprehensive analysis:\n\n");
        
        for (Map.Entry<String, String> entry : resultsByTool.entrySet()) {
            response.append("**").append(entry.getKey()).append(":**\n");
            response.append(entry.getValue()).append("\n\n");
        }
        
        response.append("**Key Insights:** ");
        response.append("The combined analysis provides multiple angles on your question. ");
        response.append("Each perspective contributes valuable information for your decision-making.");
        
        return response.toString();
    }
    
    /**
     * Create general combined response
     */
    private String createGeneralCombinedResponse(Map<String, String> resultsByTool, String originalQuery) {
        StringBuilder response = new StringBuilder();
        
        // Start with the most relevant result
        String primaryResult = resultsByTool.values().iterator().next();
        response.append(primaryResult);
        
        // Add additional information if there are multiple results
        if (resultsByTool.size() > 1) {
            response.append("\n\n**Additional Information:**\n");
            
            boolean first = true;
            for (Map.Entry<String, String> entry : resultsByTool.entrySet()) {
                if (first) {
                    first = false;
                    continue; // Skip the first one as it's already included
                }
                
                response.append("• ").append(entry.getValue()).append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * Handle case where all tools failed
     */
    private String handleAllToolFailures(List<ToolResult> toolResults, String originalQuery) {
        StringBuilder response = new StringBuilder();
        response.append("I encountered some issues while processing your request:\n\n");
        
        for (ToolResult result : toolResults) {
            if (result.getError() != null) {
                response.append("• ").append(result.getError()).append("\n");
            }
        }
        
        response.append("\nPlease try:\n");
        response.append("• Rephrasing your question\n");
        response.append("• Being more specific about what you're looking for\n");
        response.append("• Trying again in a moment\n");
        
        return response.toString();
    }
    
    /**
     * Enhanced error handling with graceful degradation and recovery suggestions
     */
    public ChatQueryResponse handleEnhancedError(Exception error, ChatQueryRequest request, long startTime) {
        String conversationId = request.getConversationId();
        
        try {
            logger.warn("Enhanced error handling for conversation {}: {}", conversationId, error.getMessage());
            
            // Attempt progressive degradation
            ChatQueryResponse degradedResponse = attemptProgressiveDegradation(request, error, startTime);
            if (degradedResponse != null) {
                return degradedResponse;
            }
            
            // Generate contextual error response
            String errorMessage = generateContextualErrorMessage(error, request);
            String recoverySuggestions = generateRecoverySuggestions(error, request);
            
            ChatQueryResponse response = new ChatQueryResponse();
            response.setConversationId(conversationId);
            response.setSuccess(false);
            response.setResponse(errorMessage + "\n\n" + recoverySuggestions);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in enhanced error handling", e);
            return createErrorResponse(conversationId, 
                    "I'm experiencing technical difficulties.", 
                    "Please try again in a moment.", 
                    startTime);
        }
    }
    
    /**
     * Attempt progressive degradation when primary processing fails
     */
    private ChatQueryResponse attemptProgressiveDegradation(ChatQueryRequest request, Exception error, long startTime) {
        try {
            // Try fallback service first
            if (fallbackService.isHelpQuery(request.getQuery())) {
                logger.info("Using fallback service for degraded response");
                return fallbackService.generateFallbackResponse(request.getQuery(), request.getConversationId(), startTime);
            }
            
            // Try simplified processing with basic tools only
            if (!(error instanceof LlmServiceException)) {
                logger.info("Attempting simplified processing");
                return attemptSimplifiedProcessing(request, startTime);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Progressive degradation also failed", e);
            return null;
        }
    }
    
    /**
     * Attempt simplified processing with only basic tools
     */
    private ChatQueryResponse attemptSimplifiedProcessing(ChatQueryRequest request, long startTime) {
        try {
            // Get only the most reliable tools
            List<ToolDefinition> basicTools = toolRegistry.getAvailableTools().stream()
                    .filter(tool -> isBasicTool(tool.getName()))
                    .collect(Collectors.toList());
            
            if (basicTools.isEmpty()) {
                return null;
            }
            
            // Try with just one basic tool
            ToolCall toolCall = new ToolCall(basicTools.get(0).getName(), new HashMap<>());
            ToolResult result = toolRegistry.executeToolCall(toolCall);
            
            if (result.isSuccess()) {
                ChatQueryResponse response = new ChatQueryResponse(
                        "I provided a simplified response: " + result.getData(), 
                        request.getConversationId());
                response.setToolsUsed(List.of(toolCall.getName()));
                response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                return response;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Simplified processing failed", e);
            return null;
        }
    }
    
    /**
     * Check if tool is considered basic/reliable
     */
    private boolean isBasicTool(String toolName) {
        return toolName.equals("tariff_lookup") || 
               toolName.equals("hs_code_finder") || 
               toolName.equals("agreement_tool");
    }
    
    // ========== PARALLEL PROCESSING METHODS ==========
    
    /**
     * Execute multiple tools in parallel for independent operations
     */
    public CompletableFuture<List<ToolResult>> executeToolsInParallel(List<ToolCall> toolCalls) {
        if (toolCalls.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        logger.debug("Executing {} tools in parallel", toolCalls.size());
        
        // Create parallel execution futures
        List<CompletableFuture<ToolResult>> futures = toolCalls.stream()
                .map(this::executeToolCallAsync)
                .collect(Collectors.toList());
        
        // Combine all futures
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * Execute single tool call asynchronously
     */
    private CompletableFuture<ToolResult> executeToolCallAsync(ToolCall toolCall) {
        return CompletableFuture.supplyAsync(() -> {
            activeParallelTasks.incrementAndGet();
            try {
                logger.debug("Executing tool {} asynchronously", toolCall.getName());
                
                if (!toolRegistry.isToolAvailable(toolCall.getName())) {
                    return new ToolResult(toolCall.getName(), false, null, 
                            "Tool is not available", System.currentTimeMillis());
                }
                
                long startTime = System.currentTimeMillis();
                ToolResult result = toolRegistry.executeToolCall(toolCall);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.debug("Tool {} completed asynchronously in {}ms", toolCall.getName(), duration);
                return result;
                
            } catch (Exception e) {
                logger.error("Error executing tool {} asynchronously", toolCall.getName(), e);
                return new ToolResult(toolCall.getName(), false, null, 
                        e.getMessage(), System.currentTimeMillis());
            } finally {
                activeParallelTasks.decrementAndGet();
            }
        }, parallelExecutor);
    }
    
    /**
     * Process query with streaming response for real-time user feedback
     */
    public CompletableFuture<ChatQueryResponse> processQueryWithStreaming(ChatQueryRequest request, 
                                                                         StreamingResponseCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String conversationId = request.getConversationId();
            
            if (conversationId == null) {
                conversationId = UUID.randomUUID().toString();
            }
            
            try {
                // Send initial status
                callback.onStatusUpdate("Analyzing your query...");
                
                // Validate input
                validateQuery(request.getQuery());
                
                // Check cache first
                callback.onStatusUpdate("Checking cache...");
                ChatQueryResponse cachedResponse = cacheService.getCachedResponse(request.getQuery());
                if (cachedResponse != null) {
                    callback.onStatusUpdate("Found cached response");
                    callback.onPartialResponse(cachedResponse.getResponse());
                    callback.onComplete();
                    return cachedResponse;
                }
                
                // Determine processing approach
                callback.onStatusUpdate("Planning execution...");
                if (requiresEnhancedProcessing(request)) {
                    return processComplexQueryWithStreaming(request, callback, startTime);
                } else {
                    return processStandardQueryWithStreaming(request, callback, startTime);
                }
                
            } catch (Exception e) {
                logger.error("Error in streaming query processing", e);
                callback.onError("Error processing your request: " + e.getMessage());
                return createErrorResponse(conversationId, e.getMessage(), 
                        "Please try again", startTime);
            }
        }, streamingExecutor);
    }
    
    /**
     * Process complex query with streaming updates
     */
    private ChatQueryResponse processComplexQueryWithStreaming(ChatQueryRequest request, 
                                                              StreamingResponseCallback callback, 
                                                              long startTime) {
        try {
            callback.onStatusUpdate("Using advanced AI processing...");
            
            // For now, delegate to AI orchestrator and provide updates
            callback.onStatusUpdate("Coordinating multiple AI agents...");
            
            // This would integrate with the AI orchestrator's streaming capabilities
            ChatQueryResponse response = aiOrchestrator.processComplexQuery(request);
            
            callback.onPartialResponse(response.getResponse());
            callback.onComplete();
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in complex streaming processing", e);
            callback.onError("Error in advanced processing: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Process standard query with streaming updates
     */
    private ChatQueryResponse processStandardQueryWithStreaming(ChatQueryRequest request, 
                                                               StreamingResponseCallback callback, 
                                                               long startTime) {
        String conversationId = request.getConversationId();
        
        try {
            // Phase 1: Tool selection
            callback.onStatusUpdate("Selecting appropriate tools...");
            ToolCall toolCall = callLlmForToolSelection(request.getQuery());
            
            // Phase 2: Tool execution
            callback.onStatusUpdate("Executing " + toolCall.getName() + "...");
            ToolResult toolResult = executeToolCall(toolCall);
            
            // Phase 3: Response generation
            callback.onStatusUpdate("Generating response...");
            String response = callLlmForResponse(request.getQuery(), toolResult);
            
            // Send partial response as it's generated
            callback.onPartialResponse(response);
            
            // Build final response
            ChatQueryResponse chatResponse = new ChatQueryResponse(response, conversationId);
            chatResponse.setToolsUsed(List.of(toolCall.getName()));
            chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            // Cache and store
            cacheService.cacheResponse(request.getQuery(), chatResponse);
            if (request.getUserId() != null) {
                conversationService.storeMessage(request.getUserId(), conversationId, request, chatResponse);
            }
            
            callback.onComplete();
            return chatResponse;
            
        } catch (Exception e) {
            logger.error("Error in standard streaming processing", e);
            callback.onError("Error processing request: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Batch process multiple similar queries for optimization
     */
    public CompletableFuture<List<ChatQueryResponse>> processBatchQueries(List<ChatQueryRequest> requests) {
        if (requests.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        logger.info("Processing batch of {} queries", requests.size());
        
        // Group similar queries for optimization
        Map<String, List<ChatQueryRequest>> groupedRequests = groupSimilarQueries(requests);
        
        // Process each group in parallel
        List<CompletableFuture<List<ChatQueryResponse>>> groupFutures = groupedRequests.entrySet().stream()
                .map(entry -> processSimilarQueriesBatch(entry.getValue()))
                .collect(Collectors.toList());
        
        // Combine all results
        return CompletableFuture.allOf(groupFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> groupFutures.stream()
                        .flatMap(future -> future.join().stream())
                        .collect(Collectors.toList()));
    }
    
    /**
     * Group similar queries for batch optimization
     */
    private Map<String, List<ChatQueryRequest>> groupSimilarQueries(List<ChatQueryRequest> requests) {
        Map<String, List<ChatQueryRequest>> groups = new HashMap<>();
        
        for (ChatQueryRequest request : requests) {
            String groupKey = determineQueryGroup(request.getQuery());
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(request);
        }
        
        return groups;
    }
    
    /**
     * Determine query group for batching
     */
    private String determineQueryGroup(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("tariff") || lowerQuery.contains("duty")) {
            return "tariff_queries";
        } else if (lowerQuery.contains("hs code") || lowerQuery.contains("classification")) {
            return "classification_queries";
        } else if (lowerQuery.contains("agreement") || lowerQuery.contains("fta")) {
            return "agreement_queries";
        } else {
            return "general_queries";
        }
    }
    
    /**
     * Process batch of similar queries with optimization
     */
    private CompletableFuture<List<ChatQueryResponse>> processSimilarQueriesBatch(List<ChatQueryRequest> similarRequests) {
        return CompletableFuture.supplyAsync(() -> {
            List<ChatQueryResponse> responses = new ArrayList<>();
            
            // Pre-warm cache and tools for this query type
            String queryType = determineQueryGroup(similarRequests.get(0).getQuery());
            prewarmForQueryType(queryType);
            
            // Process each query
            for (ChatQueryRequest request : similarRequests) {
                try {
                    ChatQueryResponse response = processQuery(request);
                    responses.add(response);
                } catch (Exception e) {
                    logger.error("Error processing query in batch", e);
                    responses.add(createErrorResponse(request.getConversationId(), 
                            "Error processing query", "Please try again", System.currentTimeMillis()));
                }
            }
            
            return responses;
        }, parallelExecutor);
    }
    
    /**
     * Pre-warm cache and tools for specific query type
     */
    private void prewarmForQueryType(String queryType) {
        try {
            logger.debug("Pre-warming for query type: {}", queryType);
            
            // Pre-load relevant tools
            switch (queryType) {
                case "tariff_queries":
                    toolRegistry.isToolAvailable("tariff_lookup");
                    break;
                case "classification_queries":
                    toolRegistry.isToolAvailable("hs_code_finder");
                    break;
                case "agreement_queries":
                    toolRegistry.isToolAvailable("agreement_tool");
                    break;
            }
            
        } catch (Exception e) {
            logger.warn("Error pre-warming for query type: {}", queryType, e);
        }
    }
    
    /**
     * Get parallel processing statistics
     */
    public ParallelProcessingStats getParallelProcessingStats() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) parallelExecutor;
        ThreadPoolExecutor streamingExec = (ThreadPoolExecutor) streamingExecutor;
        
        return new ParallelProcessingStats(
                executor.getActiveCount(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                streamingExec.getActiveCount(),
                activeParallelTasks.get(),
                batchQueue.size()
        );
    }
    
    /**
     * Shutdown parallel processing resources
     */
    public void shutdown() {
        logger.info("Shutting down parallel processing resources");
        
        parallelExecutor.shutdown();
        streamingExecutor.shutdown();
        
        try {
            if (!parallelExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                parallelExecutor.shutdownNow();
            }
            if (!streamingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                streamingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            parallelExecutor.shutdownNow();
            streamingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== SUPPORTING CLASSES ==========
    
    /**
     * Callback interface for streaming responses
     */
    public interface StreamingResponseCallback {
        void onStatusUpdate(String status);
        void onPartialResponse(String partialResponse);
        void onComplete();
        void onError(String error);
    }
    
    /**
     * Parallel processing statistics
     */
    public static class ParallelProcessingStats {
        private final int activeParallelTasks;
        private final long completedParallelTasks;
        private final long totalParallelTasks;
        private final int activeStreamingTasks;
        private final int currentParallelOperations;
        private final int batchQueueSize;
        
        public ParallelProcessingStats(int activeParallelTasks, long completedParallelTasks, 
                                     long totalParallelTasks, int activeStreamingTasks,
                                     int currentParallelOperations, int batchQueueSize) {
            this.activeParallelTasks = activeParallelTasks;
            this.completedParallelTasks = completedParallelTasks;
            this.totalParallelTasks = totalParallelTasks;
            this.activeStreamingTasks = activeStreamingTasks;
            this.currentParallelOperations = currentParallelOperations;
            this.batchQueueSize = batchQueueSize;
        }
        
        public int getActiveParallelTasks() { return activeParallelTasks; }
        public long getCompletedParallelTasks() { return completedParallelTasks; }
        public long getTotalParallelTasks() { return totalParallelTasks; }
        public int getActiveStreamingTasks() { return activeStreamingTasks; }
        public int getCurrentParallelOperations() { return currentParallelOperations; }
        public int getBatchQueueSize() { return batchQueueSize; }
        
        public double getParallelTaskCompletionRate() {
            return totalParallelTasks > 0 ? (double) completedParallelTasks / totalParallelTasks : 0.0;
        }
    }
    
    /**
     * Generate contextual error message based on error type and query
     */
    private String generateContextualErrorMessage(Exception error, ChatQueryRequest request) {
        if (error instanceof LlmServiceException) {
            return "I'm having trouble with my language processing right now.";
        } else if (error instanceof ToolExecutionException) {
            return "I encountered an issue accessing the trade data you requested.";
        } else if (error instanceof InvalidQueryException) {
            return "I had trouble understanding your question.";
        } else {
            return "I encountered an unexpected issue while processing your request.";
        }
    }
    
    /**
     * Generate recovery suggestions based on error type and query context
     */
    private String generateRecoverySuggestions(Exception error, ChatQueryRequest request) {
        StringBuilder suggestions = new StringBuilder("Here are some things you can try:\n");
        
        if (error instanceof LlmServiceException) {
            suggestions.append("• Wait a moment and try again\n");
            suggestions.append("• Try a simpler version of your question\n");
            suggestions.append("• Use the Calculator or Database for immediate results\n");
        } else if (error instanceof ToolExecutionException) {
            suggestions.append("• Check if your query includes valid country names\n");
            suggestions.append("• Try using standard country codes (US, CA, DE, etc.)\n");
            suggestions.append("• Use the manual tools while I recover\n");
        } else {
            suggestions.append("• Try rephrasing your question\n");
            suggestions.append("• Be more specific about what you're looking for\n");
            suggestions.append("• Try again in a moment\n");
        }
        
        return suggestions.toString();
    }
    
    // ========== PROGRESSIVE DEGRADATION METHODS ==========
    
    /**
     * Process query with progressive degradation when tools are unavailable
     */
    public ChatQueryResponse processQueryWithDegradation(ChatQueryRequest request) {
        long startTime = System.currentTimeMillis();
        String conversationId = request.getConversationId();
        
        try {
            // Check available tools and their health status
            List<String> unavailableTools = getUnavailableTools();
            
            if (unavailableTools.isEmpty()) {
                // All tools available, use normal processing
                return processQuery(request);
            }
            
            logger.info("Processing query with degradation - {} tools unavailable: {}", 
                    unavailableTools.size(), unavailableTools);
            
            // Attempt processing with available tools only
            ChatQueryResponse partialResponse = attemptPartialProcessing(request, unavailableTools, startTime);
            if (partialResponse != null) {
                return partialResponse;
            }
            
            // Fall back to progressive degradation
            return fallbackService.handleProgressiveDegradation(request, unavailableTools, 
                    new RuntimeException("Multiple tools unavailable"));
            
        } catch (Exception e) {
            logger.error("Error in progressive degradation processing", e);
            return fallbackService.handleProgressiveDegradation(request, 
                    List.of("all_services"), e);
        }
    }
    
    /**
     * Get list of currently unavailable tools
     */
    private List<String> getUnavailableTools() {
        List<String> unavailable = new ArrayList<>();
        
        // Check tool health status
        Map<String, ToolHealthMonitor.EnhancedToolHealthStatus> toolHealth = 
                toolHealthMonitor.getAllToolHealth();
        
        for (Map.Entry<String, ToolHealthMonitor.EnhancedToolHealthStatus> entry : toolHealth.entrySet()) {
            if (!entry.getValue().isHealthy() || entry.getValue().isAutoDisabled()) {
                unavailable.add(entry.getKey());
            }
        }
        
        // Check circuit breaker status
        Map<String, CircuitBreakerService.CircuitBreakerStatus> circuitBreakers = 
                circuitBreakerService.getAllCircuitBreakerStatuses();
        
        for (Map.Entry<String, CircuitBreakerService.CircuitBreakerStatus> entry : circuitBreakers.entrySet()) {
            if (!entry.getValue().isHealthy()) {
                unavailable.add(entry.getKey());
            }
        }
        
        return unavailable;
    }
    
    /**
     * Attempt processing with only available tools
     */
    private ChatQueryResponse attemptPartialProcessing(ChatQueryRequest request, 
                                                     List<String> unavailableTools, 
                                                     long startTime) {
        try {
            // Get available tools sorted by priority
            List<ToolDefinition> availableTools = getAvailableToolsByPriority(unavailableTools);
            
            if (availableTools.isEmpty()) {
                logger.warn("No tools available for partial processing");
                return null;
            }
            
            // Try to process with highest priority available tool
            ToolDefinition primaryTool = availableTools.get(0);
            
            logger.info("Attempting partial processing with tool: {}", primaryTool.getName());
            
            // Create tool call for primary tool
            ToolCall toolCall = createToolCallForQuery(request.getQuery(), primaryTool);
            
            // Execute with circuit breaker protection
            ToolResult result = circuitBreakerService.executeWithCircuitBreaker(
                    primaryTool.getName(),
                    () -> toolRegistry.executeToolCall(toolCall),
                    () -> createFallbackToolResult(primaryTool.getName())
            );
            
            if (result.isSuccess()) {
                // Generate response with degradation notice
                String response = generateDegradedResponse(request.getQuery(), result, unavailableTools);
                
                ChatQueryResponse chatResponse = new ChatQueryResponse(response, request.getConversationId());
                chatResponse.setToolsUsed(List.of(primaryTool.getName()));
                chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                chatResponse.setDegraded(true);
                
                // Store in conversation history
                if (request.getUserId() != null) {
                    conversationService.storeMessage(request.getUserId(), request.getConversationId(), 
                            request, chatResponse);
                }
                
                logger.info("Successfully processed with degradation using tool: {}", primaryTool.getName());
                return chatResponse;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Partial processing failed", e);
            return null;
        }
    }
    
    /**
     * Get available tools sorted by priority
     */
    private List<ToolDefinition> getAvailableToolsByPriority(List<String> unavailableTools) {
        return toolRegistry.getAvailableTools().stream()
                .filter(tool -> !unavailableTools.contains(tool.getName()))
                .filter(tool -> toolHealthMonitor.getToolAvailabilityScore(tool.getName()) > 0.5)
                .sorted((t1, t2) -> {
                    int priority1 = SERVICE_PRIORITIES.getOrDefault(t1.getName(), 10);
                    int priority2 = SERVICE_PRIORITIES.getOrDefault(t2.getName(), 10);
                    return Integer.compare(priority1, priority2);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Create appropriate tool call for query and tool
     */
    private ToolCall createToolCallForQuery(String query, ToolDefinition tool) {
        ToolCall toolCall = new ToolCall();
        toolCall.setName(tool.getName());
        
        // Create basic parameters based on tool type
        Map<String, Object> parameters = new HashMap<>();
        
        switch (tool.getName()) {
            case "tariff_lookup":
                // Extract basic parameters from query
                parameters.put("hsCode", extractHsCodeFromQuery(query));
                parameters.put("originCountry", extractOriginCountryFromQuery(query));
                parameters.put("destinationCountry", extractDestinationCountryFromQuery(query));
                break;
                
            case "hs_code_finder":
                parameters.put("productDescription", extractProductFromQuery(query));
                break;
                
            case "agreement_tool":
                parameters.put("countryCode", extractCountryFromQuery(query));
                break;
                
            default:
                // Generic parameters
                parameters.put("query", query);
                break;
        }
        
        toolCall.setArguments(parameters);
        return toolCall;
    }
    
    /**
     * Generate response with degradation notice
     */
    private String generateDegradedResponse(String query, ToolResult result, List<String> unavailableTools) {
        StringBuilder response = new StringBuilder();
        
        // Add degradation notice
        response.append("**Note:** I'm currently operating with limited functionality due to some services being unavailable.\n\n");
        
        // Add the actual response
        response.append(result.getData());
        
        // Add information about what's unavailable
        if (!unavailableTools.isEmpty()) {
            response.append("\n\n**Currently unavailable services:**\n");
            for (String tool : unavailableTools) {
                response.append("• ").append(getToolDisplayName(tool)).append("\n");
            }
        }
        
        // Add recovery information
        response.append("\n**What you can do:**\n");
        response.append("• Try again in a few minutes for full functionality\n");
        response.append("• Use the Calculator or Database for additional information\n");
        response.append("• Contact support if issues persist\n");
        
        return response.toString();
    }
    
    /**
     * Create fallback tool result when circuit breaker is open
     */
    private ToolResult createFallbackToolResult(String toolName) {
        ToolResult fallbackResult = new ToolResult();
        fallbackResult.setSuccess(true);
        fallbackResult.setData("Service temporarily unavailable. Please try the manual tools or try again later.");
        fallbackResult.setToolName(toolName);
        return fallbackResult;
    }
    
    /**
     * Get user-friendly display name for tool
     */
    private String getToolDisplayName(String toolName) {
        switch (toolName) {
            case "tariff_lookup": return "Tariff Rate Lookup";
            case "hs_code_finder": return "HS Code Classification";
            case "agreement_tool": return "Trade Agreement Information";
            case "compliance_analysis": return "Compliance Analysis";
            case "market_intelligence": return "Market Intelligence";
            case "risk_assessment": return "Risk Assessment";
            default: return toolName;
        }
    }
    
    /**
     * Communicate degraded service scenarios to users
     */
    public String createDegradationNotice(List<String> affectedServices, String estimatedRecoveryTime) {
        StringBuilder notice = new StringBuilder();
        
        notice.append("**Service Status Update**\n\n");
        notice.append("We're currently experiencing issues with some services:\n\n");
        
        for (String service : affectedServices) {
            notice.append("• ").append(getToolDisplayName(service)).append(" - Temporarily unavailable\n");
        }
        
        notice.append("\n**What's still working:**\n");
        notice.append("• Basic tariff lookups\n");
        notice.append("• Manual calculator and database access\n");
        notice.append("• Conversation history and preferences\n");
        
        if (estimatedRecoveryTime != null) {
            notice.append("\n**Estimated recovery time:** ").append(estimatedRecoveryTime);
        }
        
        notice.append("\n\nWe apologize for any inconvenience and are working to restore full functionality.");
        
        return notice.toString();
    }
    
    /**
     * Implement service priority levels for resource allocation during failures
     */
    public void adjustServicePriorities(Map<String, Double> systemLoad) {
        try {
            // Calculate available resources
            double availableCapacity = calculateAvailableCapacity(systemLoad);
            
            if (availableCapacity < 0.3) { // Less than 30% capacity
                logger.warn("Low system capacity ({:.1f}%), implementing service prioritization", 
                        availableCapacity * 100);
                
                // Disable lower priority services
                disableLowerPriorityServices(availableCapacity);
                
                // Increase resource allocation for high priority services
                prioritizeHighPriorityServices();
            }
            
        } catch (Exception e) {
            logger.error("Error adjusting service priorities", e);
        }
    }
    
    /**
     * Calculate available system capacity
     */
    private double calculateAvailableCapacity(Map<String, Double> systemLoad) {
        double cpuLoad = systemLoad.getOrDefault("cpu", 0.0);
        double memoryLoad = systemLoad.getOrDefault("memory", 0.0);
        double networkLoad = systemLoad.getOrDefault("network", 0.0);
        
        // Calculate overall capacity (inverse of load)
        return 1.0 - Math.max(Math.max(cpuLoad, memoryLoad), networkLoad);
    }
    
    /**
     * Disable lower priority services when resources are constrained
     */
    private void disableLowerPriorityServices(double availableCapacity) {
        List<String> servicesToDisable = new ArrayList<>();
        
        if (availableCapacity < 0.1) { // Critical - disable all non-essential
            servicesToDisable.addAll(List.of("risk_assessment", "market_intelligence", "compliance_analysis"));
        } else if (availableCapacity < 0.2) { // High load - disable lowest priority
            servicesToDisable.addAll(List.of("risk_assessment", "market_intelligence"));
        } else if (availableCapacity < 0.3) { // Medium load - disable lowest priority only
            servicesToDisable.add("risk_assessment");
        }
        
        for (String service : servicesToDisable) {
            toolHealthMonitor.setToolEnabled(service, false);
            logger.info("Temporarily disabled service {} due to resource constraints", service);
        }
    }
    
    /**
     * Prioritize high priority services by allocating more resources
     */
    private void prioritizeHighPriorityServices() {
        List<String> highPriorityServices = List.of("tariff_lookup", "hs_code_finder", "agreement_tool");
        
        for (String service : highPriorityServices) {
            // Ensure high priority services are enabled
            toolHealthMonitor.setToolEnabled(service, true);
            
            // Reset circuit breakers for high priority services
            circuitBreakerService.resetCircuitBreaker(service);
        }
        
        logger.info("Prioritized high priority services: {}", highPriorityServices);
    }
    
    // ========== UTILITY METHODS FOR QUERY PARSING ==========
    
    private String extractHsCodeFromQuery(String query) {
        // Simple regex to find HS codes (4-10 digits)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{4,10}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(query);
        return matcher.find() ? matcher.group() : "0101.21"; // Default fallback
    }
    
    private String extractOriginCountryFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("from china")) return "CN";
        if (lowerQuery.contains("from germany")) return "DE";
        if (lowerQuery.contains("from japan")) return "JP";
        if (lowerQuery.contains("from canada")) return "CA";
        if (lowerQuery.contains("from mexico")) return "MX";
        return "US"; // Default fallback
    }
    
    private String extractDestinationCountryFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("to us") || lowerQuery.contains("to usa")) return "US";
        if (lowerQuery.contains("to canada")) return "CA";
        if (lowerQuery.contains("to germany")) return "DE";
        if (lowerQuery.contains("to japan")) return "JP";
        if (lowerQuery.contains("to china")) return "CN";
        return "US"; // Default fallback
    }
    
    private String extractCountryFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("us") || lowerQuery.contains("usa") || lowerQuery.contains("united states")) return "US";
        if (lowerQuery.contains("canada")) return "CA";
        if (lowerQuery.contains("germany")) return "DE";
        if (lowerQuery.contains("japan")) return "JP";
        if (lowerQuery.contains("china")) return "CN";
        return "US"; // Default fallback
    }
    
    private String extractProductFromQuery(String query) {
        // Extract product description from query
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains("electronics")) return "electronics";
        if (lowerQuery.contains("automotive")) return "automotive parts";
        if (lowerQuery.contains("textiles")) return "textiles";
        if (lowerQuery.contains("machinery")) return "machinery";
        
        // Return first few words as product description
        String[] words = query.split("\\s+");
        return words.length > 0 ? String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length))) : "product";
    }
    
}