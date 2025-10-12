package com.tariffsheriff.backend.ai.reasoning;

/**
 * Ranking of a scenario with score
 */
public class ScenarioRanking {
    private final String scenarioId;
    private final double score;
    
    public ScenarioRanking(String scenarioId, double score) {
        this.scenarioId = scenarioId;
        this.score = score;
    }
    
    @Override
    public String toString() {
        return String.format("Ranking[%s]: %.2f", scenarioId, score);
    }
    
    // Getters
    public String getScenarioId() { return scenarioId; }
    public double getScore() { return score; }
}