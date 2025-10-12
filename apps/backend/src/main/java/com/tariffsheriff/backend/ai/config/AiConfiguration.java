package com.tariffsheriff.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for AI system parameters
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfiguration {
    
    private Orchestrator orchestrator = new Orchestrator();
    private Context context = new Context();
    private Planning planning = new Planning();
    private Reasoning reasoning = new Reasoning();
    
    public static class Orchestrator {
        private int maxConcurrentQueries = 100;
        private int queryTimeoutSeconds = 300;
        private int contextRetentionHours = 24;
        
        // Getters and setters
        public int getMaxConcurrentQueries() { return maxConcurrentQueries; }
        public void setMaxConcurrentQueries(int maxConcurrentQueries) { this.maxConcurrentQueries = maxConcurrentQueries; }
        
        public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) { this.queryTimeoutSeconds = queryTimeoutSeconds; }
        
        public int getContextRetentionHours() { return contextRetentionHours; }
        public void setContextRetentionHours(int contextRetentionHours) { this.contextRetentionHours = contextRetentionHours; }
    }
    
    public static class Context {
        private int maxContextHistory = 10;
        private int maxConversationsPerUser = 50;
        private int maxMessagesPerConversation = 100;
        
        // Getters and setters
        public int getMaxContextHistory() { return maxContextHistory; }
        public void setMaxContextHistory(int maxContextHistory) { this.maxContextHistory = maxContextHistory; }
        
        public int getMaxConversationsPerUser() { return maxConversationsPerUser; }
        public void setMaxConversationsPerUser(int maxConversationsPerUser) { this.maxConversationsPerUser = maxConversationsPerUser; }
        
        public int getMaxMessagesPerConversation() { return maxMessagesPerConversation; }
        public void setMaxMessagesPerConversation(int maxMessagesPerConversation) { this.maxMessagesPerConversation = maxMessagesPerConversation; }
    }
    
    public static class Planning {
        private int maxExecutionSteps = 10;
        private int stepTimeoutSeconds = 60;
        private double complexityThreshold = 0.5;
        
        // Getters and setters
        public int getMaxExecutionSteps() { return maxExecutionSteps; }
        public void setMaxExecutionSteps(int maxExecutionSteps) { this.maxExecutionSteps = maxExecutionSteps; }
        
        public int getStepTimeoutSeconds() { return stepTimeoutSeconds; }
        public void setStepTimeoutSeconds(int stepTimeoutSeconds) { this.stepTimeoutSeconds = stepTimeoutSeconds; }
        
        public double getComplexityThreshold() { return complexityThreshold; }
        public void setComplexityThreshold(double complexityThreshold) { this.complexityThreshold = complexityThreshold; }
    }
    
    public static class Reasoning {
        private int maxInferenceDepth = 10;
        private double confidenceThreshold = 0.6;
        private boolean enableScenarioAnalysis = true;
        
        // Getters and setters
        public int getMaxInferenceDepth() { return maxInferenceDepth; }
        public void setMaxInferenceDepth(int maxInferenceDepth) { this.maxInferenceDepth = maxInferenceDepth; }
        
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        
        public boolean isEnableScenarioAnalysis() { return enableScenarioAnalysis; }
        public void setEnableScenarioAnalysis(boolean enableScenarioAnalysis) { this.enableScenarioAnalysis = enableScenarioAnalysis; }
    }
    
    // Main getters and setters
    public Orchestrator getOrchestrator() { return orchestrator; }
    public void setOrchestrator(Orchestrator orchestrator) { this.orchestrator = orchestrator; }
    
    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }
    
    public Planning getPlanning() { return planning; }
    public void setPlanning(Planning planning) { this.planning = planning; }
    
    public Reasoning getReasoning() { return reasoning; }
    public void setReasoning(Reasoning reasoning) { this.reasoning = reasoning; }
}