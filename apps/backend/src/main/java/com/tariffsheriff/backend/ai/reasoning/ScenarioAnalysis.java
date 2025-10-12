package com.tariffsheriff.backend.ai.reasoning;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scenario analysis and comparison
 */
public class ScenarioAnalysis {
    private final List<Scenario> scenarios;
    private final List<ScenarioComparison> comparisons;
    private final List<ScenarioRanking> rankings;
    private final List<String> insights;
    
    public ScenarioAnalysis(List<Scenario> scenarios, List<ScenarioComparison> comparisons,
                           List<ScenarioRanking> rankings, List<String> insights) {
        this.scenarios = new ArrayList<>(scenarios);
        this.comparisons = new ArrayList<>(comparisons);
        this.rankings = new ArrayList<>(rankings);
        this.insights = new ArrayList<>(insights);
    }
    
    /**
     * Get best scenario based on rankings
     */
    public Scenario getBestScenario() {
        if (rankings.isEmpty()) return null;
        
        String bestId = rankings.get(0).getScenarioId();
        return scenarios.stream()
                .filter(s -> s.getId().equals(bestId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get comparison between two specific scenarios
     */
    public ScenarioComparison getComparison(String scenario1Id, String scenario2Id) {
        return comparisons.stream()
                .filter(comp -> (comp.getScenario1Id().equals(scenario1Id) && comp.getScenario2Id().equals(scenario2Id)) ||
                               (comp.getScenario1Id().equals(scenario2Id) && comp.getScenario2Id().equals(scenario1Id)))
                .findFirst()
                .orElse(null);
    }
    
    // Getters
    public List<Scenario> getScenarios() { return new ArrayList<>(scenarios); }
    public List<ScenarioComparison> getComparisons() { return new ArrayList<>(comparisons); }
    public List<ScenarioRanking> getRankings() { return new ArrayList<>(rankings); }
    public List<String> getInsights() { return new ArrayList<>(insights); }
}