package com.tariffsheriff.backend.ai.reasoning;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of logical reasoning process
 */
public class ReasoningResult {
    private final List<Fact> derivedFacts;
    private final List<Inference> inferences;
    private final double confidence;
    private final int inferenceDepth;
    
    public ReasoningResult(List<Fact> derivedFacts, List<Inference> inferences, 
                          double confidence, int inferenceDepth) {
        this.derivedFacts = new ArrayList<>(derivedFacts);
        this.inferences = new ArrayList<>(inferences);
        this.confidence = confidence;
        this.inferenceDepth = inferenceDepth;
    }
    
    /**
     * Get facts derived at specific depth
     */
    public List<Fact> getFactsAtDepth(int depth) {
        return inferences.stream()
                .filter(inf -> inf.getDepth() == depth)
                .map(Inference::getFact)
                .toList();
    }
    
    /**
     * Get high confidence facts
     */
    public List<Fact> getHighConfidenceFacts() {
        return derivedFacts.stream()
                .filter(fact -> fact.getConfidence() >= 0.8)
                .toList();
    }
    
    /**
     * Check if reasoning was successful
     */
    public boolean isSuccessful() {
        return confidence >= 0.6 && !inferences.isEmpty();
    }
    
    // Getters
    public List<Fact> getDerivedFacts() { return new ArrayList<>(derivedFacts); }
    public List<Inference> getInferences() { return new ArrayList<>(inferences); }
    public double getConfidence() { return confidence; }
    public int getInferenceDepth() { return inferenceDepth; }
}