package com.tariffsheriff.backend.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for chatbot tools
 */
@Component
@ConfigurationProperties(prefix = "chatbot.tools")
public class ToolProperties {
    
    /**
     * Global tool configuration
     */
    @Min(value = 1000, message = "Default timeout must be at least 1000ms")
    private int defaultTimeoutMs = 10000;
    
    @Min(value = 1, message = "Max concurrent executions must be at least 1")
    private int maxConcurrentExecutions = 5;
    
    private boolean enableHealthChecks = true;
    
    @Min(value = 30000, message = "Health check interval must be at least 30 seconds")
    private int healthCheckIntervalMs = 300000; // 5 minutes
    
    /**
     * Individual tool configurations
     */
    @NotNull
    private Map<String, ToolConfig> tools = new HashMap<>();
    
    public ToolProperties() {
        // Initialize default tool configurations
        initializeDefaultToolConfigs();
    }
    
    private void initializeDefaultToolConfigs() {
        // Tariff Lookup Tool
        ToolConfig tariffTool = new ToolConfig();
        tariffTool.setEnabled(true);
        tariffTool.setTimeoutMs(10000);
        tariffTool.setMaxRetries(2);
        tariffTool.setDescription("Get MFN and preferential tariff rates for specific trade routes");
        tools.put("getTariffRateLookup", tariffTool);
        
        // HS Code Finder Tool
        ToolConfig hsCodeTool = new ToolConfig();
        hsCodeTool.setEnabled(true);
        hsCodeTool.setTimeoutMs(5000);
        hsCodeTool.setMaxRetries(1);
        hsCodeTool.setDescription("Find HS codes based on product descriptions");
        tools.put("findHsCodeForProduct", hsCodeTool);
        
        // Agreement Tool
        ToolConfig agreementTool = new ToolConfig();
        agreementTool.setEnabled(true);
        agreementTool.setTimeoutMs(5000);
        agreementTool.setMaxRetries(1);
        agreementTool.setDescription("Get trade agreements for specific countries");
        tools.put("getAgreementsByCountry", agreementTool);
    }
    
    // Getters and setters
    public int getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }
    
    public void setDefaultTimeoutMs(int defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }
    
    public int getMaxConcurrentExecutions() {
        return maxConcurrentExecutions;
    }
    
    public void setMaxConcurrentExecutions(int maxConcurrentExecutions) {
        this.maxConcurrentExecutions = maxConcurrentExecutions;
    }
    
    public boolean isEnableHealthChecks() {
        return enableHealthChecks;
    }
    
    public void setEnableHealthChecks(boolean enableHealthChecks) {
        this.enableHealthChecks = enableHealthChecks;
    }
    
    public int getHealthCheckIntervalMs() {
        return healthCheckIntervalMs;
    }
    
    public void setHealthCheckIntervalMs(int healthCheckIntervalMs) {
        this.healthCheckIntervalMs = healthCheckIntervalMs;
    }
    
    public Map<String, ToolConfig> getTools() {
        return tools;
    }
    
    public void setTools(Map<String, ToolConfig> tools) {
        this.tools = tools;
    }
    
    /**
     * Get configuration for a specific tool
     */
    public ToolConfig getToolConfig(String toolName) {
        return tools.getOrDefault(toolName, createDefaultToolConfig());
    }
    
    private ToolConfig createDefaultToolConfig() {
        ToolConfig defaultConfig = new ToolConfig();
        defaultConfig.setEnabled(true);
        defaultConfig.setTimeoutMs(defaultTimeoutMs);
        defaultConfig.setMaxRetries(1);
        defaultConfig.setDescription("Default tool configuration");
        return defaultConfig;
    }
    
    /**
     * Configuration for individual tools
     */
    public static class ToolConfig {
        private boolean enabled = true;
        
        @Min(value = 1000, message = "Tool timeout must be at least 1000ms")
        private int timeoutMs = 10000;
        
        @Min(value = 0, message = "Max retries cannot be negative")
        private int maxRetries = 1;
        
        private String description = "";
        
        private Map<String, Object> parameters = new HashMap<>();
        
        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getTimeoutMs() {
            return timeoutMs;
        }
        
        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}