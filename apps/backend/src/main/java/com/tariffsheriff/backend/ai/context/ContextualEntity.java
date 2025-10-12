package com.tariffsheriff.backend.ai.context;

import java.time.LocalDateTime;

/**
 * Entity extracted from conversation with type and confidence
 */
public class ContextualEntity {
    private final String type;
    private final String value;
    private final double confidence;
    private LocalDateTime lastMentioned;
    private int mentionCount;
    
    public ContextualEntity(String type, String value, double confidence) {
        this.type = type;
        this.value = value;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp between 0 and 1
        this.lastMentioned = LocalDateTime.now();
        this.mentionCount = 1;
    }
    
    // Alternative constructor for ConversationService usage
    public ContextualEntity(String type, String value, LocalDateTime lastMentioned, int mentionCount) {
        this.type = type;
        this.value = value;
        this.confidence = 0.8; // Default confidence
        this.lastMentioned = lastMentioned;
        this.mentionCount = mentionCount;
    }
    
    /**
     * Check if entity matches another entity
     */
    public boolean matches(ContextualEntity other) {
        return this.type.equals(other.type) && 
               this.value.equalsIgnoreCase(other.value);
    }
    
    /**
     * Check if entity is high confidence
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }
    
    /**
     * Check if entity is medium confidence
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }
    
    /**
     * Check if entity is low confidence
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s (%.2f)", type, value, confidence);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ContextualEntity that = (ContextualEntity) obj;
        return type.equals(that.type) && value.equalsIgnoreCase(that.value);
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() + value.toLowerCase().hashCode();
    }
    
    /**
     * Increment mention count and update last mentioned time
     */
    public void incrementMentionCount() {
        this.mentionCount++;
        this.lastMentioned = LocalDateTime.now();
    }
    
    /**
     * Update last mentioned time
     */
    public void updateLastMentioned(LocalDateTime time) {
        this.lastMentioned = time;
    }
    
    // Getters
    public String getType() { return type; }
    public String getValue() { return value; }
    public double getConfidence() { return confidence; }
    public LocalDateTime getLastMentioned() { return lastMentioned; }
    public int getMentionCount() { return mentionCount; }
}