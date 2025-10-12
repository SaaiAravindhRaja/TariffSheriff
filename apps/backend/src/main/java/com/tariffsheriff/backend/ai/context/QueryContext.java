package com.tariffsheriff.backend.ai.context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Context for a specific query including conversation history and references
 */
public class QueryContext {
    private final String query;
    private final String conversationId;
    private final UserContext userContext;
    private final List<ContextualMessage> conversationHistory;
    private final List<ContextualEntity> referencedEntities;
    private final LocalDateTime timestamp;
    
    public QueryContext(String query, String conversationId, UserContext userContext,
                       List<ContextualMessage> conversationHistory, 
                       List<ContextualEntity> referencedEntities, LocalDateTime timestamp) {
        this.query = query;
        this.conversationId = conversationId;
        this.userContext = userContext;
        this.conversationHistory = new ArrayList<>(conversationHistory);
        this.referencedEntities = new ArrayList<>(referencedEntities);
        this.timestamp = timestamp;
    }
    
    /**
     * Add entities discovered during processing
     */
    public void addEntities(List<ContextualEntity> entities) {
        this.referencedEntities.addAll(entities);
    }
    
    /**
     * Add single entity
     */
    public void addEntity(ContextualEntity entity) {
        this.referencedEntities.add(entity);
    }
    
    /**
     * Get entities by type
     */
    public List<ContextualEntity> getEntitiesByType(String type) {
        return referencedEntities.stream()
                .filter(entity -> type.equals(entity.getType()))
                .toList();
    }
    
    /**
     * Check if context contains entity
     */
    public boolean hasEntity(String type, String value) {
        return referencedEntities.stream()
                .anyMatch(entity -> type.equals(entity.getType()) && 
                         value.equalsIgnoreCase(entity.getValue()));
    }
    
    /**
     * Get most recent messages of specific role
     */
    public List<ContextualMessage> getMessagesByRole(String role, int limit) {
        return conversationHistory.stream()
                .filter(msg -> role.equals(msg.getRole()))
                .skip(Math.max(0, conversationHistory.size() - limit))
                .toList();
    }
    
    // Getters
    public String getQuery() { return query; }
    public String getConversationId() { return conversationId; }
    public UserContext getUserContext() { return userContext; }
    public List<ContextualMessage> getConversationHistory() { return new ArrayList<>(conversationHistory); }
    public List<ContextualEntity> getReferencedEntities() { return new ArrayList<>(referencedEntities); }
    public LocalDateTime getTimestamp() { return timestamp; }
}