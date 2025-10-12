package com.tariffsheriff.backend.ai.context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Message with contextual information and extracted entities
 */
public class ContextualMessage {
    private final String role;
    private final String content;
    private final LocalDateTime timestamp;
    private final List<ContextualEntity> entities;
    
    public ContextualMessage(String role, String content, LocalDateTime timestamp, 
                           List<ContextualEntity> entities) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.entities = new ArrayList<>(entities);
    }
    
    /**
     * Add entity to message
     */
    public void addEntity(ContextualEntity entity) {
        entities.add(entity);
    }
    
    /**
     * Get entities by type
     */
    public List<ContextualEntity> getEntitiesByType(String type) {
        return entities.stream()
                .filter(entity -> type.equals(entity.getType()))
                .toList();
    }
    
    /**
     * Check if message contains entity
     */
    public boolean hasEntity(String type, String value) {
        return entities.stream()
                .anyMatch(entity -> type.equals(entity.getType()) && 
                         value.equalsIgnoreCase(entity.getValue()));
    }
    
    // Getters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<ContextualEntity> getEntities() { return new ArrayList<>(entities); }
}