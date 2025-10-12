package com.tariffsheriff.backend.ai.reasoning;

/**
 * Represents a conclusion drawn from analysis
 */
public class Conclusion {
    private final String statement;
    private final String reasoning;
    private final double evidenceStrength;
    
    public Conclusion(String statement, String reasoning, double evidenceStrength) {
        this.statement = statement;
        this.reasoning = reasoning;
        this.evidenceStrength = Math.max(0.0, Math.min(1.0, evidenceStrength));
    }
    
    @Override
    public String toString() {
        return String.format("Conclusion: %s (evidence: %.2f)", statement, evidenceStrength);
    }
    
    // Getters
    public String getStatement() { return statement; }
    public String getReasoning() { return reasoning; }
    public double getEvidenceStrength() { return evidenceStrength; }
}