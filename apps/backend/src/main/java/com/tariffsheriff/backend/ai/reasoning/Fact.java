package com.tariffsheriff.backend.ai.reasoning;

/**
 * Represents a fact in the reasoning system
 */
public class Fact {
    private final String predicate;
    private final String subject;
    private final String object;
    private final double confidence;
    
    public Fact(String predicate, String subject, String object, double confidence) {
        this.predicate = predicate;
        this.subject = subject;
        this.object = object;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    public Fact(String predicate, String subject, String object) {
        this(predicate, subject, object, 1.0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Fact fact = (Fact) obj;
        return predicate.equals(fact.predicate) &&
               subject.equals(fact.subject) &&
               (object != null ? object.equals(fact.object) : fact.object == null);
    }
    
    @Override
    public int hashCode() {
        int result = predicate.hashCode();
        result = 31 * result + subject.hashCode();
        result = 31 * result + (object != null ? object.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s, %s) [%.2f]", predicate, subject, object, confidence);
    }
    
    // Getters
    public String getPredicate() { return predicate; }
    public String getSubject() { return subject; }
    public String getObject() { return object; }
    public double getConfidence() { return confidence; }
}