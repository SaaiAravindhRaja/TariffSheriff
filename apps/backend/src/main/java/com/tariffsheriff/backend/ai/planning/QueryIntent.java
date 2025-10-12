package com.tariffsheriff.backend.ai.planning;

/**
 * Enumeration of possible query intents
 */
public enum QueryIntent {
    TARIFF_LOOKUP("Lookup tariff rates and duties"),
    COST_ANALYSIS("Analyze total costs and landed costs"),
    COMPLIANCE_CHECK("Check regulatory compliance requirements"),
    RISK_ASSESSMENT("Assess trade risks and vulnerabilities"),
    MARKET_ANALYSIS("Analyze market trends and opportunities"),
    OPTIMIZATION("Optimize trade strategies and routes"),
    COMPARISON("Compare multiple options or scenarios"),
    PRODUCT_CLASSIFICATION("Classify products and find HS codes"),
    GENERAL_INQUIRY("General trade-related inquiry");
    
    private final String description;
    
    QueryIntent(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if intent requires data analysis
     */
    public boolean requiresDataAnalysis() {
        return this == COST_ANALYSIS || this == MARKET_ANALYSIS || 
               this == RISK_ASSESSMENT || this == COMPARISON;
    }
    
    /**
     * Check if intent requires external data
     */
    public boolean requiresExternalData() {
        return this == MARKET_ANALYSIS || this == RISK_ASSESSMENT || 
               this == COMPLIANCE_CHECK;
    }
    
    /**
     * Check if intent involves calculations
     */
    public boolean involvesCalculations() {
        return this == TARIFF_LOOKUP || this == COST_ANALYSIS || 
               this == OPTIMIZATION;
    }
}