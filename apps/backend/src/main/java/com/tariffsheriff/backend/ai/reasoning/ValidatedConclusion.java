package com.tariffsheriff.backend.ai.reasoning;

import java.util.ArrayList;
import java.util.List;

/**
 * A conclusion with validation information
 */
public class ValidatedConclusion {
    private final Conclusion conclusion;
    private final double confidence;
    private final List<String> supportingEvidence;
    private final List<String> contradictingEvidence;
    
    public ValidatedConclusion(Conclusion conclusion, double confidence,
                              List<String> supportingEvidence, List<String> contradictingEvidence) {
        this.conclusion = conclusion;
        this.confidence = confidence;
        this.supportingEvidence = new ArrayList<>(supportingEvidence);
        this.contradictingEvidence = new ArrayList<>(contradictingEvidence);
    }
    
    /**
     * Check if conclusion is well-supported
     */
    public boolean isWellSupported() {
        return confidence >= 0.8 && !supportingEvidence.isEmpty() && contradictingEvidence.isEmpty();
    }
    
    /**
     * Check if conclusion has conflicts
     */
    public boolean hasConflicts() {
        return !contradictingEvidence.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ValidatedConclusion: %s (confidence: %.2f, support: %d, conflicts: %d)",
                conclusion.getStatement(), confidence, supportingEvidence.size(), contradictingEvidence.size());
    }
    
    // Getters
    public Conclusion getConclusion() { return conclusion; }
    public double getConfidence() { return confidence; }
    public List<String> getSupportingEvidence() { return new ArrayList<>(supportingEvidence); }
    public List<String> getContradictingEvidence() { return new ArrayList<>(contradictingEvidence); }
}