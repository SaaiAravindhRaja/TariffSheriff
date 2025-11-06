package com.tariffsheriff.backend.chatbot.dto;

import java.time.LocalDateTime;

/**
 * Error response DTO for chat-related errors
 */
public class ChatErrorResponse {
    
    private String error;
    private String message;
    private String suggestion;
    private LocalDateTime timestamp;
    private String conversationId;
    
    public ChatErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatErrorResponse(String error, String message) {
        this();
        this.error = error;
        this.message = message;
    }
    
    public ChatErrorResponse(String error, String message, String suggestion) {
        this(error, message);
        this.suggestion = suggestion;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}