package com.tariffsheriff.backend.ai.reasoning;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reasoning rule with conditions and conclusion
 */
public class Rule {
    private final String id;
    private final List<RuleCondition> conditions;
    private final Fact conclusion;
    private final double confidence;
    
    public Rule(String id, List<RuleCondition> conditions, Fact conclusion, double confidence) {
        this.id = id;
        this.conditions = new ArrayList<>(conditions);
        this.conclusion = conclusion;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    public Rule(String id, List<RuleCondition> conditions, Fact conclusion) {
        this(id, conditions, conclusion, 1.0);
    }
    
    /**
     * Check if all conditions are satisfied by the given facts
     */
    public boolean isApplicable(List<Fact> facts) {
        return conditions.stream().allMatch(condition -> 
            facts.stream().anyMatch(fact -> condition.matches(fact)));
    }
    
    @Override
    public String toString() {
        return String.format("Rule[%s]: IF %s THEN %s [%.2f]", 
                id, conditions, conclusion, confidence);
    }
    
    // Getters
    public String getId() { return id; }
    public List<RuleCondition> getConditions() { return new ArrayList<>(conditions); }
    public Fact getConclusion() { return conclusion; }
    public double getConfidence() { return confidence; }
}