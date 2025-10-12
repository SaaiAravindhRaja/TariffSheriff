package com.tariffsheriff.backend.ai.reasoning;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a scenario for analysis and comparison
 */
public class Scenario {
    private final String id;
    private final String description;
    private final Map<String, Double> metrics;
    private final Map<String, String> attributes;
    
    public Scenario(String id, String description) {
        this.id = id;
        this.description = description;
        this.metrics = new HashMap<>();
        this.attributes = new HashMap<>();
    }
    
    /**
     * Add metric to scenario
     */
    public void addMetric(String name, double value) {
        metrics.put(name, value);
    }
    
    /**
     * Add attribute to scenario
     */
    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }
    
    /**
     * Get metric value
     */
    public double getMetric(String name) {
        return metrics.getOrDefault(name, 0.0);
    }
    
    /**
     * Check if scenario has metric
     */
    public boolean hasMetric(String name) {
        return metrics.containsKey(name);
    }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String name) {
        return attributes.get(name);
    }
    
    @Override
    public String toString() {
        return String.format("Scenario[%s]: %s", id, description);
    }
    
    // Getters
    public String getId() { return id; }
    public String getDescription() { return description; }
    public Map<String, Double> getMetrics() { return new HashMap<>(metrics); }
    public Map<String, String> getAttributes() { return new HashMap<>(attributes); }
}