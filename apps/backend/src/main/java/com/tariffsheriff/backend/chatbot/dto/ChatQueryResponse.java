package com.tariffsheriff.backend.chatbot.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for chat queries containing the AI assistant's response
 */
public class ChatQueryResponse {
    
    private String response;
    private String conversationId;
    private LocalDateTime timestamp;
    private List<String> toolsUsed;
    private Long processingTimeMs;
    private boolean success;
    private boolean cached;
    private boolean degraded;
    private Double confidence;
    
    public ChatQueryResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }
    
    public ChatQueryResponse(String response) {
        this();
        this.response = response;
    }
    
    public ChatQueryResponse(String response, String conversationId) {
        this(response);
        this.conversationId = conversationId;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<String> getToolsUsed() {
        return toolsUsed;
    }
    
    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isCached() {
        return cached;
    }
    
    public void setCached(boolean cached) {
        this.cached = cached;
    }
    
    public boolean isDegraded() {
        return degraded;
    }
    
    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get tool results (alias for toolsUsed for backward compatibility)
     */
    public List<String> getToolResults() {
        return toolsUsed;
    }
    
    /**
     * Set tool results (alias for toolsUsed for backward compatibility)
     */
    public void setToolResults(List<String> toolResults) {
        this.toolsUsed = toolResults;
    }
}