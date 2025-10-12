package com.tariffsheriff.backend.auth.model;

import java.util.Set;
import java.util.EnumSet;

/**
 * AI-specific roles with predefined permission sets
 */
public enum AiRole {
    BASIC_USER("Basic User", EnumSet.of(
        AiPermission.BASIC_CHAT,
        AiPermission.TARIFF_LOOKUP,
        AiPermission.HS_CODE_FINDER
    )),
    
    TRADE_PROFESSIONAL("Trade Professional", EnumSet.of(
        AiPermission.BASIC_CHAT,
        AiPermission.ADVANCED_CHAT,
        AiPermission.TARIFF_LOOKUP,
        AiPermission.HS_CODE_FINDER,
        AiPermission.AGREEMENT_ANALYSIS,
        AiPermission.COMPLIANCE_ANALYSIS,
        AiPermission.HISTORICAL_DATA,
        AiPermission.EXPORT_CONVERSATIONS,
        AiPermission.BOOKMARK_RESPONSES
    )),
    
    TRADE_ANALYST("Trade Analyst", EnumSet.of(
        AiPermission.BASIC_CHAT,
        AiPermission.ADVANCED_CHAT,
        AiPermission.TARIFF_LOOKUP,
        AiPermission.HS_CODE_FINDER,
        AiPermission.AGREEMENT_ANALYSIS,
        AiPermission.COMPLIANCE_ANALYSIS,
        AiPermission.MARKET_INTELLIGENCE,
        AiPermission.RISK_ASSESSMENT,
        AiPermission.COMPLEX_REASONING,
        AiPermission.SCENARIO_MODELING,
        AiPermission.REAL_TIME_DATA,
        AiPermission.HISTORICAL_DATA,
        AiPermission.MARKET_DATA,
        AiPermission.EXPORT_CONVERSATIONS,
        AiPermission.SHARE_ANALYSIS,
        AiPermission.BOOKMARK_RESPONSES
    )),
    
    ENTERPRISE_USER("Enterprise User", EnumSet.of(
        AiPermission.BASIC_CHAT,
        AiPermission.ADVANCED_CHAT,
        AiPermission.TARIFF_LOOKUP,
        AiPermission.HS_CODE_FINDER,
        AiPermission.AGREEMENT_ANALYSIS,
        AiPermission.COMPLIANCE_ANALYSIS,
        AiPermission.MARKET_INTELLIGENCE,
        AiPermission.RISK_ASSESSMENT,
        AiPermission.MULTI_AGENT_ORCHESTRATION,
        AiPermission.COMPLEX_REASONING,
        AiPermission.SCENARIO_MODELING,
        AiPermission.PREDICTIVE_ANALYTICS,
        AiPermission.REAL_TIME_DATA,
        AiPermission.HISTORICAL_DATA,
        AiPermission.MARKET_DATA,
        AiPermission.REGULATORY_DATA,
        AiPermission.EXPORT_CONVERSATIONS,
        AiPermission.SHARE_ANALYSIS,
        AiPermission.BOOKMARK_RESPONSES
    )),
    
    ADMIN("Administrator", EnumSet.allOf(AiPermission.class));

    private final String displayName;
    private final Set<AiPermission> permissions;

    AiRole(String displayName, Set<AiPermission> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<AiPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(AiPermission permission) {
        return permissions.contains(permission);
    }

    public static AiRole fromString(String role) {
        if (role == null) return BASIC_USER;
        
        try {
            return valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BASIC_USER;
        }
    }
}