package com.tariffsheriff.backend.chatbot.dto;

import java.util.Map;

/**
 * Defines a tool that can be called by the LLM
 */
public class ToolDefinition {
    
    private String name;
    private String description;
    private Map<String, Object> parameters;
    private boolean enabled;
    private int timeoutMs;
    
    public ToolDefinition() {
        this.enabled = true;
        this.timeoutMs = 10000; // Default 10 second timeout
    }
    
    public ToolDefinition(String name, String description, Map<String, Object> parameters) {
        this();
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
}