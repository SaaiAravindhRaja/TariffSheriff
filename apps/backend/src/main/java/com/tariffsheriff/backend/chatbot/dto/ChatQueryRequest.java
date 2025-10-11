package com.tariffsheriff.backend.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for chat queries submitted by users
 */
public class ChatQueryRequest {
    
    @NotBlank(message = "Query cannot be empty")
    @Size(max = 2000, message = "Query cannot exceed 2000 characters")
    private String query;
    
    private String conversationId;
    
    public ChatQueryRequest() {}
    
    public ChatQueryRequest(String query) {
        this.query = query;
    }
    
    public ChatQueryRequest(String query, String conversationId) {
        this.query = query;
        this.conversationId = conversationId;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}