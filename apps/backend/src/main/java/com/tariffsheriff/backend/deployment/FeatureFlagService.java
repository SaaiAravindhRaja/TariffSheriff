package com.tariffsheriff.backend.deployment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for managing feature flags for enhanced AI capabilities
 * Supports gradual rollout and A/B testing of new features
 */
@Service
public class FeatureFlagService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);
    
    @Value("${ai.features.enhanced-orchestration.enabled:false}")
    private boolean enhancedOrchestrationEnabled;
    
    @Value("${ai.features.enhanced-orchestration.rollout-percentage:0}")
    private int enhancedOrchestrationRollout;
    
    @Value("${ai.features.advanced-reasoning.enabled:false}")
    private boolean advancedReasoningEnabled;
    
    @Value("${ai.features.advanced-reasoning.rollout-percentage:0}")
    private int advancedReasoningRollout;
    
    @Value("${ai.features.real-time-data.enabled:false}")
    private boolean realTimeDataEnabled;
    
    @Value("${ai.features.real-time-data.rollout-percentage:0}")
    private int realTimeDataRollout;
    
    @Value("${ai.features.enhanced-ui.enabled:false}")
    private boolean enhancedUiEnabled;
    
    @Value("${ai.features.enhanced-ui.rollout-percentage:0}")
    private int enhancedUiRollout;
    
    private final Map<String, Boolean> userFeatureCache = new ConcurrentHashMap<>();
    private final Set<String> betaUsers = new HashSet<>();
    
    public enum FeatureFlag {
        ENHANCED_ORCHESTRATION("enhanced-orchestration"),
        ADVANCED_REASONING("advanced-reasoning"),
        REAL_TIME_DATA("real-time-data"),
        ENHANCED_UI("enhanced-ui"),
        MULTI_AGENT_COORDINATION("multi-agent-coordination"),
        CONTEXTUAL_MEMORY("contextual-memory"),
        PROACTIVE_INTELLIGENCE("proactive-intelligence"),
        VOICE_INTERACTION("voice-interaction"),
        ADVANCED_VISUALIZATION("advanced-visualization");
        
        private final String flagName;
        
        FeatureFlag(String flagName) {
            this.flagName = flagName;
        }
        
        public String getFlagName() {
            return flagName;
        }
    }
    
    /**
     * Check if a feature is enabled for a specific user
     */
    @Cacheable(value = "featureFlags", key = "#userId + '_' + #feature.flagName")
    public boolean isFeatureEnabled(String userId, FeatureFlag feature) {
        try {
            // Check if user is in beta program
            if (betaUsers.contains(userId)) {
                logger.debug("Feature {} enabled for beta user {}", feature.getFlagName(), userId);
                return true;
            }
            
            // Check global feature flag and rollout percentage
            boolean globalEnabled = isGloballyEnabled(feature);
            if (!globalEnabled) {
                return false;
            }
            
            // Check rollout percentage
            int rolloutPercentage = getRolloutPercentage(feature);
            if (rolloutPercentage >= 100) {
                return true;
            }
            
            // Use consistent hash for user-based rollout
            int userHash = Math.abs(userId.hashCode() % 100);
            boolean enabled = userHash < rolloutPercentage;
            
            logger.debug("Feature {} {} for user {} (hash: {}, rollout: {}%)", 
                feature.getFlagName(), enabled ? "enabled" : "disabled", 
                userId, userHash, rolloutPercentage);
            
            return enabled;
            
        } catch (Exception e) {
            logger.error("Error checking feature flag {} for user {}: {}", 
                feature.getFlagName(), userId, e.getMessage());
            return false; // Fail closed
        }
    }
    
    /**
     * Check if a feature is globally enabled
     */
    public boolean isGloballyEnabled(FeatureFlag feature) {
        switch (feature) {
            case ENHANCED_ORCHESTRATION:
                return enhancedOrchestrationEnabled;
            case ADVANCED_REASONING:
                return advancedReasoningEnabled;
            case REAL_TIME_DATA:
                return realTimeDataEnabled;
            case ENHANCED_UI:
                return enhancedUiEnabled;
            default:
                return false;
        }
    }
    
    /**
     * Get rollout percentage for a feature
     */
    public int getRolloutPercentage(FeatureFlag feature) {
        switch (feature) {
            case ENHANCED_ORCHESTRATION:
                return enhancedOrchestrationRollout;
            case ADVANCED_REASONING:
                return advancedReasoningRollout;
            case REAL_TIME_DATA:
                return realTimeDataRollout;
            case ENHANCED_UI:
                return enhancedUiRollout;
            default:
                return 0;
        }
    }
    
    /**
     * Add user to beta program
     */
    public void addBetaUser(String userId) {
        betaUsers.add(userId);
        userFeatureCache.clear(); // Clear cache to refresh flags
        logger.info("Added user {} to beta program", userId);
    }
    
    /**
     * Remove user from beta program
     */
    public void removeBetaUser(String userId) {
        betaUsers.remove(userId);
        userFeatureCache.clear(); // Clear cache to refresh flags
        logger.info("Removed user {} from beta program", userId);
    }
    
    /**
     * Get all enabled features for a user
     */
    public Map<String, Boolean> getUserFeatures(String userId) {
        Map<String, Boolean> features = new ConcurrentHashMap<>();
        for (FeatureFlag flag : FeatureFlag.values()) {
            features.put(flag.getFlagName(), isFeatureEnabled(userId, flag));
        }
        return features;
    }
    
    /**
     * Clear feature flag cache
     */
    public void clearCache() {
        userFeatureCache.clear();
        logger.info("Feature flag cache cleared");
    }
}