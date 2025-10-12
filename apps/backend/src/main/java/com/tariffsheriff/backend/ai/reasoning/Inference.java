package com.tariffsheriff.backend.ai.reasoning;

/**
 * Represents an inference made during reasoning
 */
public class Inference {
    private final Rule rule;
    private final Fact fact;
    private final int depth;
    
    public Inference(Rule rule, Fact fact, int depth) {
        this.rule = rule;
        this.fact = fact;
        this.depth = depth;
    }
    
    @Override
    public String toString() {
        return String.format("Inference[depth=%d]: %s -> %s", depth, rule.getId(), fact);
    }
    
    // Getters
    public Rule getRule() { return rule; }
    public Fact getFact() { return fact; }
    public int getDepth() { return depth; }
}