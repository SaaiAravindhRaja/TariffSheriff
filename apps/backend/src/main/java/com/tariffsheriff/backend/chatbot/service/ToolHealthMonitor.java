package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.config.ToolProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors the health and performance of chatbot tools
 */
@Service
public class ToolHealthMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolHealthMonitor.class);
    
    @Autowired
    private ToolProperties toolProperties;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    // Health metrics per tool
    private final Map<String, ToolHealthMetrics> healthMetrics = new ConcurrentHashMap<>();
    
    /**
     * Record a successful tool execution
     */
    public void recordSuccess(String toolName, long executionTimeMs) {
        ToolHealthMetrics metrics = getOrCreateMetrics(toolName);
        metrics.recordSuccess(executionTimeMs);
    }
    
    /**
     * Record a failed tool execution
     */
    public void recordFailure(String toolName, String errorMessage) {
        ToolHealthMetrics metrics = getOrCreateMetrics(toolName);
        metrics.recordFailure(errorMessage);
    }
    
    /**
     * Get health status for a specific tool
     */
    public ToolHealthStatus getToolHealth(String toolName) {
        ToolHealthMetrics metrics = healthMetrics.get(toolName);
        if (metrics == null) {
            return new ToolHealthStatus(toolName, true, "No metrics available", 0, 0, 0);
        }
        
        return metrics.getHealthStatus();
    }
    
    /**
     * Get health status for all tools
     */
    public Map<String, ToolHealthStatus> getAllToolHealth() {
        Map<String, ToolHealthStatus> healthStatuses = new HashMap<>();
        
        for (Map.Entry<String, ToolHealthMetrics> entry : healthMetrics.entrySet()) {
            healthStatuses.put(entry.getKey(), entry.getValue().getHealthStatus());
        }
        
        return healthStatuses;
    }
    
    /**
     * Scheduled health check for all tools
     */
    @Scheduled(fixedRateString = "#{@toolProperties.healthCheckIntervalMs}")
    public void performHealthChecks() {
        if (!toolProperties.isEnableHealthChecks()) {
            return;
        }
        
        logger.debug("Starting scheduled tool health checks");
        
        for (String toolName : toolRegistry.getAvailableTools().stream()
                .map(tool -> tool.getName()).toList()) {
            
            try {
                performToolHealthCheck(toolName);
            } catch (Exception e) {
                logger.error("Error during health check for tool {}: {}", toolName, e.getMessage());
                recordFailure(toolName, "Health check failed: " + e.getMessage());
            }
        }
        
        logger.debug("Completed scheduled tool health checks");
    }
    
    /**
     * Perform health check for a specific tool
     */
    private void performToolHealthCheck(String toolName) {
        // Create a simple health check call
        ToolCall healthCheck = createHealthCheckCall(toolName);
        
        long startTime = System.currentTimeMillis();
        ToolResult result = toolRegistry.executeToolCall(healthCheck);
        long executionTime = System.currentTimeMillis() - startTime;
        
        if (result.isSuccess()) {
            recordSuccess(toolName, executionTime);
            logger.debug("Health check passed for tool: {}", toolName);
        } else {
            recordFailure(toolName, "Health check failed: " + result.getError());
            logger.warn("Health check failed for tool {}: {}", toolName, result.getError());
        }
    }
    
    /**
     * Create a health check call for a tool
     */
    private ToolCall createHealthCheckCall(String toolName) {
        ToolCall healthCheck = new ToolCall();
        healthCheck.setName(toolName);
        
        // Create minimal parameters for health check
        Map<String, Object> parameters = new HashMap<>();
        
        switch (toolName) {
            case "getTariffRateLookup":
                parameters.put("hsCode", "0101.21");
                parameters.put("originCountry", "US");
                parameters.put("destinationCountry", "CA");
                break;
            case "findHsCodeForProduct":
                parameters.put("productDescription", "test");
                break;
            case "getAgreementsByCountry":
                parameters.put("countryCode", "US");
                break;
            default:
                // Generic health check
                break;
        }
        
        healthCheck.setParameters(parameters);
        return healthCheck;
    }
    
    private ToolHealthMetrics getOrCreateMetrics(String toolName) {
        return healthMetrics.computeIfAbsent(toolName, k -> new ToolHealthMetrics(toolName));
    }
    
    /**
     * Internal class to track health metrics for a tool
     */
    private static class ToolHealthMetrics {
        private final String toolName;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile String lastError = null;
        private volatile LocalDateTime lastErrorTime = null;
        private volatile LocalDateTime lastSuccessTime = null;
        
        public ToolHealthMetrics(String toolName) {
            this.toolName = toolName;
        }
        
        public void recordSuccess(long executionTimeMs) {
            successCount.incrementAndGet();
            totalExecutionTime.addAndGet(executionTimeMs);
            lastSuccessTime = LocalDateTime.now();
        }
        
        public void recordFailure(String errorMessage) {
            failureCount.incrementAndGet();
            lastError = errorMessage;
            lastErrorTime = LocalDateTime.now();
        }
        
        public ToolHealthStatus getHealthStatus() {
            int successes = successCount.get();
            int failures = failureCount.get();
            int totalCalls = successes + failures;
            
            double successRate = totalCalls > 0 ? (double) successes / totalCalls : 1.0;
            long avgExecutionTime = successes > 0 ? totalExecutionTime.get() / successes : 0;
            
            boolean isHealthy = successRate >= 0.8 && (lastErrorTime == null || 
                (lastSuccessTime != null && lastSuccessTime.isAfter(lastErrorTime)));
            
            String status = isHealthy ? "Healthy" : 
                (lastError != null ? "Unhealthy: " + lastError : "Unhealthy");
            
            return new ToolHealthStatus(toolName, isHealthy, status, 
                successRate, avgExecutionTime, totalCalls);
        }
    }
    
    /**
     * Health status information for a tool
     */
    public static class ToolHealthStatus {
        private final String toolName;
        private final boolean healthy;
        private final String status;
        private final double successRate;
        private final long averageExecutionTimeMs;
        private final int totalExecutions;
        
        public ToolHealthStatus(String toolName, boolean healthy, String status, 
                              double successRate, long averageExecutionTimeMs, int totalExecutions) {
            this.toolName = toolName;
            this.healthy = healthy;
            this.status = status;
            this.successRate = successRate;
            this.averageExecutionTimeMs = averageExecutionTimeMs;
            this.totalExecutions = totalExecutions;
        }
        
        // Getters
        public String getToolName() { return toolName; }
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public double getSuccessRate() { return successRate; }
        public long getAverageExecutionTimeMs() { return averageExecutionTimeMs; }
        public int getTotalExecutions() { return totalExecutions; }
    }
}