package com.tariffsheriff.backend.chatbot.dto;

/**
 * Standardized result from tool execution
 */
public class ToolResult {
    
    private boolean success;
    private String data;
    private String error;
    private String toolName;
    private long executionTimeMs;
    
    public ToolResult() {}
    
    public ToolResult(boolean success, String data) {
        this.success = success;
        this.data = data;
    }
    
    public ToolResult(String toolName, boolean success, String data) {
        this.toolName = toolName;
        this.success = success;
        this.data = data;
    }
    
    /**
     * Create a successful result
     */
    public static ToolResult success(String toolName, String data) {
        return new ToolResult(toolName, true, data);
    }
    
    /**
     * Create an error result
     */
    public static ToolResult error(String toolName, String error) {
        ToolResult result = new ToolResult(toolName, false, null);
        result.setError(error);
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}