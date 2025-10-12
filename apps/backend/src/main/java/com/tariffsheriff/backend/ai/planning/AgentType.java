package com.tariffsheriff.backend.ai.planning;

/**
 * Types of specialized AI agents available for processing
 */
public enum AgentType {
    TARIFF_ANALYSIS("Tariff Analysis Agent", "Analyzes tariff rates, duties, and trade costs"),
    COMPLIANCE("Compliance Agent", "Checks regulatory compliance and documentation requirements"),
    RISK_ASSESSMENT("Risk Assessment Agent", "Evaluates trade risks and vulnerabilities"),
    MARKET_INTELLIGENCE("Market Intelligence Agent", "Analyzes market trends and opportunities"),
    OPTIMIZATION("Optimization Agent", "Optimizes trade strategies and recommendations");
    
    private final String displayName;
    private final String description;
    
    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if agent requires external data sources
     */
    public boolean requiresExternalData() {
        return this == MARKET_INTELLIGENCE || this == RISK_ASSESSMENT || this == COMPLIANCE;
    }
    
    /**
     * Check if agent performs calculations
     */
    public boolean performsCalculations() {
        return this == TARIFF_ANALYSIS || this == OPTIMIZATION;
    }
    
    /**
     * Get estimated processing time in seconds
     */
    public int getEstimatedProcessingTime() {
        return switch (this) {
            case TARIFF_ANALYSIS -> 10;
            case COMPLIANCE -> 15;
            case RISK_ASSESSMENT -> 20;
            case MARKET_INTELLIGENCE -> 25;
            case OPTIMIZATION -> 30;
        };
    }
}