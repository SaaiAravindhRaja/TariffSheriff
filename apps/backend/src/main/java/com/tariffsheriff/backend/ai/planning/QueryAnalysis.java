package com.tariffsheriff.backend.ai.planning;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Analysis result for a query including intent, complexity, and required agents
 */
public class QueryAnalysis {
    private final String query;
    private final QueryIntent primaryIntent;
    private final List<QueryIntent> secondaryIntents;
    private final ComplexityLevel complexity;
    private final List<AgentType> requiredAgents;
    private final List<QueryEntity> entities;
    
    public QueryAnalysis(String query, QueryIntent primaryIntent, List<QueryIntent> secondaryIntents,
                        ComplexityLevel complexity, List<AgentType> requiredAgents, List<QueryEntity> entities) {
        this.query = query;
        this.primaryIntent = primaryIntent;
        this.secondaryIntents = new ArrayList<>(secondaryIntents);
        this.complexity = complexity;
        this.requiredAgents = new ArrayList<>(requiredAgents);
        this.entities = new ArrayList<>(entities);
    }
    
    // Additional constructor for simple analysis
    public QueryAnalysis(String query, ComplexityLevel complexity, boolean requiresMultiAgent, 
                        List<QueryEntity> entities, List<QueryIntent> intents) {
        this.query = query;
        this.complexity = complexity;
        this.entities = new ArrayList<>(entities != null ? entities : Collections.emptyList());
        this.secondaryIntents = new ArrayList<>(intents != null ? intents : Collections.emptyList());
        this.primaryIntent = !this.secondaryIntents.isEmpty() ? this.secondaryIntents.get(0) : QueryIntent.GENERAL_INQUIRY;
        
        // Determine required agents based on multi-agent flag
        if (requiresMultiAgent) {
            this.requiredAgents = List.of(AgentType.TARIFF_ANALYSIS, AgentType.OPTIMIZATION);
        } else {
            this.requiredAgents = List.of(AgentType.TARIFF_ANALYSIS);
        }
    }
    
    /**
     * Check if analysis indicates multi-agent processing needed
     */
    public boolean requiresMultiAgent() {
        return requiredAgents.size() > 1 || complexity == ComplexityLevel.HIGH;
    }
    
    /**
     * Check if analysis indicates comparison operation
     */
    public boolean isComparison() {
        return primaryIntent == QueryIntent.COMPARISON || 
               secondaryIntents.contains(QueryIntent.COMPARISON);
    }
    
    /**
     * Get entities by type
     */
    public List<QueryEntity> getEntitiesByType(String type) {
        return entities.stream()
                .filter(entity -> type.equals(entity.getType()))
                .toList();
    }
    
    /**
     * Check if query involves specific entity
     */
    public boolean hasEntity(String type, String value) {
        return entities.stream()
                .anyMatch(entity -> type.equals(entity.getType()) && 
                         value.equalsIgnoreCase(entity.getValue()));
    }
    
    /**
     * Check if analysis indicates contextual references
     */
    public boolean hasContextualReferences() {
        // Default implementation - can be overridden in enhanced analysis
        return false;
    }
    
    /**
     * Get extracted entities for compatibility
     */
    public List<QueryEntity> getExtractedEntities() {
        return getEntities();
    }
    
    // Getters
    public String getQuery() { return query; }
    public QueryIntent getPrimaryIntent() { return primaryIntent; }
    public List<QueryIntent> getSecondaryIntents() { return new ArrayList<>(secondaryIntents); }
    public ComplexityLevel getComplexity() { return complexity; }
    public List<AgentType> getRequiredAgents() { return new ArrayList<>(requiredAgents); }
    public List<QueryEntity> getEntities() { return new ArrayList<>(entities); }
}