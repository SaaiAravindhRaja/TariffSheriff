package com.tariffsheriff.backend.ai.orchestration;

import com.tariffsheriff.backend.ai.context.ContextManager;
import com.tariffsheriff.backend.ai.context.QueryContext;
import com.tariffsheriff.backend.ai.context.UserContext;
import com.tariffsheriff.backend.ai.planning.ExecutionPlan;
import com.tariffsheriff.backend.ai.planning.QueryPlanner;
import com.tariffsheriff.backend.ai.reasoning.ReasoningEngine;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.service.FallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central orchestrator for AI operations, coordinating multiple agents and services
 * to handle complex queries with multi-step analysis and reasoning.
 */
@Service
public class AiOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(AiOrchestrator.class);
    
    private final ContextManager contextManager;
    private final QueryPlanner queryPlanner;
    private final ReasoningEngine reasoningEngine;
    private final FallbackService fallbackService;
    
    @Autowired
    public AiOrchestrator(ContextManager contextManager, 
                         QueryPlanner queryPlanner,
                         ReasoningEngine reasoningEngine,
                         FallbackService fallbackService) {
        this.contextManager = contextManager;
        this.queryPlanner = queryPlanner;
        this.reasoningEngine = reasoningEngine;
        this.fallbackService = fallbackService;
    }
    
    /**
     * Process complex queries with multi-agent coordination
     */
    public ChatQueryResponse processComplexQuery(ChatQueryRequest request) {
        long startTime = System.currentTimeMillis();
        String conversationId = request.getConversationId();
        String userId = request.getUserId();
        
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }
        
        try {
            logger.info("Processing complex query for conversation {}: {}", conversationId, request.getQuery());
            
            // Load user context and conversation history
            UserContext userContext = contextManager.loadUserContext(userId);
            QueryContext queryContext = contextManager.buildQueryContext(request, userContext);
            
            // Analyze query and create execution plan
            ExecutionPlan executionPlan = queryPlanner.createExecutionPlan(request.getQuery(), queryContext);
            
            // Execute plan with agent coordination
            List<AgentResult> agentResults = coordinateAgents(executionPlan);
            
            // Synthesize results using reasoning engine
            String synthesizedResponse = synthesizeResults(agentResults, queryContext);
            
            // Update context with new information
            contextManager.updateContext(conversationId, queryContext, request, synthesizedResponse);
            
            // Build response
            ChatQueryResponse response = new ChatQueryResponse(synthesizedResponse, conversationId);
            response.setToolsUsed(extractToolsUsed(agentResults));
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Successfully processed complex query for conversation {} in {}ms", 
                    conversationId, response.getProcessingTimeMs());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing complex query for conversation {}", conversationId, e);
            return handleFailures(List.of(new AgentFailure("orchestrator", e.getMessage(), e)), 
                                conversationId, startTime);
        }
    }
    
    /**
     * Coordinate multiple agents based on execution plan
     */
    public List<AgentResult> coordinateAgents(ExecutionPlan executionPlan) {
        List<AgentResult> results = new ArrayList<>();
        List<AgentFailure> failures = new ArrayList<>();
        
        try {
            // Execute steps in dependency order
            for (ExecutionPlan.ExecutionStep step : executionPlan.getSteps()) {
                try {
                    logger.debug("Executing step: {} with agent: {}", step.getStepId(), step.getAgentType());
                    
                    // For now, simulate agent execution - actual agents will be implemented in later tasks
                    AgentResult result = simulateAgentExecution(step);
                    results.add(result);
                    
                } catch (Exception e) {
                    logger.warn("Step {} failed: {}", step.getStepId(), e.getMessage());
                    failures.add(new AgentFailure(step.getAgentType().toString(), e.getMessage(), e));
                    
                    // Continue with other steps if possible
                    if (step.isRequired()) {
                        throw new ChatbotException("Required step failed: " + step.getStepId(), e);
                    }
                }
            }
            
            // Handle partial failures gracefully
            if (!failures.isEmpty()) {
                logger.warn("Some steps failed but continuing with available results: {} failures", failures.size());
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("Agent coordination failed", e);
            throw new ChatbotException("Failed to coordinate agents", e);
        }
    }
    
    /**
     * Synthesize results from multiple agents into coherent response
     */
    public String synthesizeResults(List<AgentResult> results, QueryContext context) {
        try {
            if (results.isEmpty()) {
                return "I apologize, but I wasn't able to gather enough information to provide a comprehensive answer.";
            }
            
            // Use reasoning engine to combine results
            return reasoningEngine.synthesizeResults(results, context);
            
        } catch (Exception e) {
            logger.error("Error synthesizing results", e);
            
            // Fallback to simple concatenation
            StringBuilder response = new StringBuilder();
            for (AgentResult result : results) {
                if (result.isSuccess() && result.getResult() != null) {
                    response.append(result.getResult()).append("\n\n");
                }
            }
            
            return response.length() > 0 ? response.toString().trim() : 
                   "I encountered some issues processing your request, but I'm working to improve my responses.";
        }
    }
    
    /**
     * Handle failures with graceful degradation
     */
    public ChatQueryResponse handleFailures(List<AgentFailure> failures, String conversationId, long startTime) {
        try {
            logger.info("Attempting graceful degradation for {} failures", failures.size());
            
            // Try fallback service for simple queries
            String fallbackQuery = extractOriginalQuery(failures);
            if (fallbackQuery != null && fallbackService.isHelpQuery(fallbackQuery)) {
                return fallbackService.generateFallbackResponse(fallbackQuery, conversationId, startTime);
            }
            
            // Generate error response with recovery suggestions
            String errorMessage = generateErrorMessage(failures);
            String suggestions = generateRecoverySuggestions(failures);
            
            ChatQueryResponse response = new ChatQueryResponse();
            response.setConversationId(conversationId);
            response.setSuccess(false);
            response.setResponse(errorMessage + "\n\n" + suggestions);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in failure handling", e);
            
            ChatQueryResponse response = new ChatQueryResponse();
            response.setConversationId(conversationId);
            response.setSuccess(false);
            response.setResponse("I'm experiencing technical difficulties. Please try again in a moment.");
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return response;
        }
    }
    
    /**
     * Simulate agent execution for development phase
     */
    private AgentResult simulateAgentExecution(ExecutionPlan.ExecutionStep step) {
        // This is a placeholder - actual agent implementations will replace this
        return new AgentResult(
            step.getAgentType().toString(),
            step.getStepId(),
            true,
            "Simulated result for " + step.getDescription(),
            List.of("Simulated insight"),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Extract tools used from agent results
     */
    private List<String> extractToolsUsed(List<AgentResult> results) {
        List<String> tools = new ArrayList<>();
        for (AgentResult result : results) {
            if (result.isSuccess()) {
                tools.add(result.getAgentId());
            }
        }
        return tools;
    }
    
    /**
     * Extract original query from failures for fallback
     */
    private String extractOriginalQuery(List<AgentFailure> failures) {
        // This would be enhanced to extract the original query
        return null;
    }
    
    /**
     * Generate user-friendly error message
     */
    private String generateErrorMessage(List<AgentFailure> failures) {
        if (failures.size() == 1) {
            return "I encountered an issue while processing your request: " + failures.get(0).getMessage();
        } else {
            return "I encountered " + failures.size() + " issues while processing your complex request.";
        }
    }
    
    /**
     * Generate recovery suggestions
     */
    private String generateRecoverySuggestions(List<AgentFailure> failures) {
        return "Please try:\n" +
               "• Simplifying your question\n" +
               "• Breaking it into smaller parts\n" +
               "• Trying again in a moment\n" +
               "• Contacting support if the issue persists";
    }
    
    /**
     * Agent result data model
     */
    public static class AgentResult {
        private final String agentId;
        private final String taskId;
        private final boolean success;
        private final Object result;
        private final List<String> insights;
        private final long executionTime;
        
        public AgentResult(String agentId, String taskId, boolean success, Object result, 
                          List<String> insights, long executionTime) {
            this.agentId = agentId;
            this.taskId = taskId;
            this.success = success;
            this.result = result;
            this.insights = insights != null ? new ArrayList<>(insights) : new ArrayList<>();
            this.executionTime = executionTime;
        }
        
        // Getters
        public String getAgentId() { return agentId; }
        public String getTaskId() { return taskId; }
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public List<String> getInsights() { return new ArrayList<>(insights); }
        public long getExecutionTime() { return executionTime; }
    }
    
    /**
     * Agent failure data model
     */
    public static class AgentFailure {
        private final String agentId;
        private final String message;
        private final Throwable cause;
        
        public AgentFailure(String agentId, String message, Throwable cause) {
            this.agentId = agentId;
            this.message = message;
            this.cause = cause;
        }
        
        // Getters
        public String getAgentId() { return agentId; }
        public String getMessage() { return message; }
        public Throwable getCause() { return cause; }
    }
}