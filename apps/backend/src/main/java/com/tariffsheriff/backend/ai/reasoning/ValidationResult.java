package com.tariffsheriff.backend.ai.reasoning;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of conclusion validation
 */
public class ValidationResult {
    private final List<ValidatedConclusion> validatedConclusions;
    private final double overallConfidence;
    private final boolean isValid;
    
    public ValidationResult(List<ValidatedConclusion> validatedConclusions, 
                           double overallConfidence, boolean isValid) {
        this.validatedConclusions = new ArrayList<>(validatedConclusions);
        this.overallConfidence = overallConfidence;
        this.isValid = isValid;
    }
    
    /**
     * Get high confidence conclusions
     */
    public List<ValidatedConclusion> getHighConfidenceConclusions() {
        return validatedConclusions.stream()
                .filter(vc -> vc.getConfidence() >= 0.8)
                .toList();
    }
    
    /**
     * Get conclusions with contradicting evidence
     */
    public List<ValidatedConclusion> getConclusionsWithContradictions() {
        return validatedConclusions.stream()
                .filter(vc -> !vc.getContradictingEvidence().isEmpty())
                .toList();
    }
    
    // Getters
    public List<ValidatedConclusion> getValidatedConclusions() { return new ArrayList<>(validatedConclusions); }
    public double getOverallConfidence() { return overallConfidence; }
    public boolean isValid() { return isValid; }
}