package com.tariffsheriff.backend.ai.reasoning;

import java.util.HashMap;
import java.util.Map;

/**
 * Comparison between two scenarios
 */
public class ScenarioComparison {
    private final String scenario1Id;
    private final String scenario2Id;
    private final Map<String, Double> differences;
    private final String winner;
    
    public ScenarioComparison(String scenario1Id, String scenario2Id, 
                             Map<String, Double> differences, String winner) {
        this.scenario1Id = scenario1Id;
        this.scenario2Id = scenario2Id;
        this.differences = new HashMap<>(differences);
        this.winner = winner;
    }
    
    /**
     * Get difference for specific metric
     */
    public double getDifference(String metric) {
        return differences.getOrDefault(metric, 0.0);
    }
    
    /**
     * Check if comparison has significant difference
     */
    public boolean hasSignificantDifference(String metric, double threshold) {
        return Math.abs(getDifference(metric)) > threshold;
    }
    
    @Override
    public String toString() {
        return String.format("Comparison[%s vs %s]: winner=%s", scenario1Id, scenario2Id, winner);
    }
    
    // Getters
    public String getScenario1Id() { return scenario1Id; }
    public String getScenario2Id() { return scenario2Id; }
    public Map<String, Double> getDifferences() { return new HashMap<>(differences); }
    public String getWinner() { return winner; }
}