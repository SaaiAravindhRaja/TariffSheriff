package com.tariffsheriff.backend.auth.model;

/**
 * AI-specific permissions for fine-grained access control
 */
public enum AiPermission {
    // Basic AI capabilities
    BASIC_CHAT("ai:chat:basic", "Basic chat functionality"),
    ADVANCED_CHAT("ai:chat:advanced", "Advanced chat with context"),
    
    // Tool access permissions
    TARIFF_LOOKUP("ai:tool:tariff", "Access to tariff lookup tools"),
    HS_CODE_FINDER("ai:tool:hscode", "Access to HS code finder tools"),
    AGREEMENT_ANALYSIS("ai:tool:agreement", "Access to agreement analysis tools"),
    COMPLIANCE_ANALYSIS("ai:tool:compliance", "Access to compliance analysis tools"),
    MARKET_INTELLIGENCE("ai:tool:market", "Access to market intelligence tools"),
    RISK_ASSESSMENT("ai:tool:risk", "Access to risk assessment tools"),
    
    // Advanced AI features
    MULTI_AGENT_ORCHESTRATION("ai:orchestration:multi", "Multi-agent orchestration"),
    COMPLEX_REASONING("ai:reasoning:complex", "Complex reasoning capabilities"),
    SCENARIO_MODELING("ai:scenario:modeling", "Scenario modeling and analysis"),
    PREDICTIVE_ANALYTICS("ai:analytics:predictive", "Predictive analytics"),
    
    // Data access permissions
    REAL_TIME_DATA("ai:data:realtime", "Access to real-time data feeds"),
    HISTORICAL_DATA("ai:data:historical", "Access to historical data"),
    MARKET_DATA("ai:data:market", "Access to market data"),
    REGULATORY_DATA("ai:data:regulatory", "Access to regulatory data"),
    
    // Export and sharing permissions
    EXPORT_CONVERSATIONS("ai:export:conversations", "Export conversation history"),
    SHARE_ANALYSIS("ai:share:analysis", "Share analysis results"),
    BOOKMARK_RESPONSES("ai:bookmark:responses", "Bookmark AI responses"),
    
    // Administrative permissions
    VIEW_USAGE_ANALYTICS("ai:admin:usage", "View AI usage analytics"),
    MANAGE_AI_SETTINGS("ai:admin:settings", "Manage AI system settings"),
    MONITOR_AI_HEALTH("ai:admin:health", "Monitor AI system health");

    private final String permission;
    private final String description;

    AiPermission(String permission, String description) {
        this.permission = permission;
        this.description = description;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return permission;
    }
}