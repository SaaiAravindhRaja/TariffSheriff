package com.tariffsheriff.backend.ai.reasoning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic analysis result with metrics and flags
 */
public class AnalysisResult {
    private final Map<String, Double> metrics;
    private final Set<String> flags;
    private final Map<String, Object> data;
    
    public AnalysisResult() {
        this.metrics = new HashMap<>();
        this.flags = new HashSet<>();
        this.data = new HashMap<>();
    }
    
    /**
     * Add metric to result
     */
    public void addMetric(String name, double value) {
        metrics.put(name, value);
    }
    
    /**
     * Add flag to result
     */
    public void addFlag(String flag) {
        flags.add(flag);
    }
    
    /**
     * Add data to result
     */
    public void addData(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * Get metric value
     */
    public double getMetric(String name) {
        return metrics.getOrDefault(name, 0.0);
    }
    
    /**
     * Check if result has metric
     */
    public boolean hasMetric(String name) {
        return metrics.containsKey(name);
    }
    
    /**
     * Check if result has flag
     */
    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }
    
    /**
     * Get data value
     */
    public Object getData(String key) {
        return data.get(key);
    }
    
    // Getters
    public Map<String, Double> getMetrics() { return new HashMap<>(metrics); }
    public Set<String> getFlags() { return new HashSet<>(flags); }
    public Map<String, Object> getData() { return new HashMap<>(data); }
}