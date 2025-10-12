package com.tariffsheriff.backend.ai.planning;

/**
 * Query complexity levels for resource allocation and processing strategy
 */
public enum ComplexityLevel {
    LOW("Simple, single-intent queries"),
    MEDIUM("Multi-step or multi-entity queries"),
    HIGH("Complex analysis requiring multiple agents and reasoning");
    
    private final String description;
    
    ComplexityLevel(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get processing timeout based on complexity
     */
    public int getTimeoutSeconds() {
        return switch (this) {
            case LOW -> 30;
            case MEDIUM -> 60;
            case HIGH -> 120;
        };
    }
    
    /**
     * Get resource allocation multiplier
     */
    public double getResourceMultiplier() {
        return switch (this) {
            case LOW -> 1.0;
            case MEDIUM -> 2.0;
            case HIGH -> 3.0;
        };
    }
    
    /**
     * Check if complexity requires multi-agent processing
     */
    public boolean requiresMultiAgent() {
        return this == MEDIUM || this == HIGH;
    }
}