package com.tariffsheriff.backend.ai.context;

import java.util.HashMap;
import java.util.Map;

/**
 * User preferences for personalized AI responses
 */
public class UserPreferences {
    private final Map<String, Integer> preferenceCounters;
    private final Map<String, String> settings;
    
    public UserPreferences() {
        this.preferenceCounters = new HashMap<>();
        this.settings = new HashMap<>();
        initializeDefaults();
    }
    
    /**
     * Initialize default preferences
     */
    private void initializeDefaults() {
        settings.put("response_detail", "medium"); // low, medium, high
        settings.put("preferred_currency", "USD");
        settings.put("preferred_units", "metric");
        settings.put("include_explanations", "true");
        settings.put("show_confidence_scores", "false");
    }
    
    /**
     * Increment preference counter
     */
    public void incrementPreference(String preference) {
        preferenceCounters.merge(preference, 1, Integer::sum);
    }
    
    /**
     * Get preference count
     */
    public int getPreferenceCount(String preference) {
        return preferenceCounters.getOrDefault(preference, 0);
    }
    
    /**
     * Set user setting
     */
    public void setSetting(String key, String value) {
        settings.put(key, value);
    }
    
    /**
     * Get user setting
     */
    public String getSetting(String key) {
        return settings.get(key);
    }
    
    /**
     * Get user setting with default
     */
    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
    
    /**
     * Check if user prefers detailed responses
     */
    public boolean prefersDetailedResponses() {
        return "high".equals(getSetting("response_detail"));
    }
    
    /**
     * Check if user wants explanations included
     */
    public boolean includeExplanations() {
        return "true".equals(getSetting("include_explanations"));
    }
    
    /**
     * Check if user wants confidence scores shown
     */
    public boolean showConfidenceScores() {
        return "true".equals(getSetting("show_confidence_scores"));
    }
    
    /**
     * Get preferred currency
     */
    public String getPreferredCurrency() {
        return getSetting("preferred_currency", "USD");
    }
    
    /**
     * Get all preference counters
     */
    public Map<String, Integer> getAllPreferenceCounts() {
        return new HashMap<>(preferenceCounters);
    }
    
    /**
     * Get all settings
     */
    public Map<String, String> getAllSettings() {
        return new HashMap<>(settings);
    }
    
    /**
     * Increment country preference
     */
    public void incrementCountryPreference(String country) {
        incrementPreference("country_" + country);
    }
    
    /**
     * Increment product preference
     */
    public void incrementProductPreference(String product) {
        incrementPreference("product_" + product);
    }
    
    /**
     * Increment format preference
     */
    public void incrementFormatPreference(String format) {
        incrementPreference("format_" + format);
    }
    
    /**
     * Increment complexity preference
     */
    public void incrementComplexityPreference(String complexity) {
        incrementPreference("complexity_" + complexity);
    }
}