package com.tariffsheriff.backend.ai.planning;

/**
 * Entity extracted from query for planning purposes
 */
public class QueryEntity {
    private final String type;
    private final String value;
    private final double confidence;
    
    public QueryEntity(String type, String value, double confidence) {
        this.type = type;
        this.value = value;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Check if entity is high confidence
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }
    
    /**
     * Check if entity matches another entity
     */
    public boolean matches(QueryEntity other) {
        return this.type.equals(other.type) && 
               this.value.equalsIgnoreCase(other.value);
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s (%.2f)", type, value, confidence);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        QueryEntity that = (QueryEntity) obj;
        return type.equals(that.type) && value.equalsIgnoreCase(that.value);
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() + value.toLowerCase().hashCode();
    }
    
    // Getters
    public String getType() { return type; }
    public String getValue() { return value; }
    public double getConfidence() { return confidence; }
}