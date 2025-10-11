package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when tool execution fails
 */
public class ToolExecutionException extends ChatbotException {
    
    private String toolName;
    
    public ToolExecutionException(String toolName, String message) {
        super(message, "Please try rephrasing your question or check if the information you're looking for exists.");
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(message, "Please try rephrasing your question or check if the information you're looking for exists.", cause);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion) {
        super(message, suggestion);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion, Throwable cause) {
        super(message, suggestion, cause);
        this.toolName = toolName;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
}