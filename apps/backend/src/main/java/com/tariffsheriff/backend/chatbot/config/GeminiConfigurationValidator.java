package com.tariffsheriff.backend.chatbot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates Gemini configuration on application startup
 */
@Component
public class GeminiConfigurationValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiConfigurationValidator.class);
    
    @Autowired
    private GeminiProperties geminiProperties;
    
    /**
     * Validates Gemini configuration after bean construction
     * Uses @PostConstruct to run validation early in the application lifecycle
     */
    @PostConstruct
    public void validateConfiguration() {
        logger.info("=== Validating Gemini Configuration ===");
        
        try {
            validateApiKey();
            validateModel();
            validateBaseUrl();
            validateParameters();
            
            // Log configuration at startup without exposing the API key
            logger.info("✓ Gemini configuration validation completed successfully");
            logger.info("Configuration details:");
            logger.info("  - Model: {}", geminiProperties.getModel());
            logger.info("  - Base URL: {}", geminiProperties.getBaseUrl());
            logger.info("  - Max Tokens: {}", geminiProperties.getMaxTokens());
            logger.info("  - Temperature: {}", geminiProperties.getTemperature());
            logger.info("  - Timeout: {}ms", geminiProperties.getTimeoutMs());
            logger.info("  - API Key: Configured ({}...)", maskApiKey(geminiProperties.getApiKey()));
            
        } catch (Exception e) {
            logger.error("✗ Gemini configuration validation FAILED: {}", e.getMessage());
            throw new IllegalStateException("Invalid Gemini configuration", e);
        }
    }
    
    /**
     * Masks the API key for safe logging
     * Shows only the first 4 characters followed by asterisks
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****";
    }
    
    private void validateApiKey() {
        String apiKey = geminiProperties.getApiKey();
        
        // Check API key presence
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("GEMINI API KEY NOT CONFIGURED: API key is required but not set");
        }
        
        // Check for placeholder values
        if ("your_api_key_here".equals(apiKey) || "your-api-key-here".equals(apiKey)) {
            throw new IllegalArgumentException("GEMINI API KEY NOT CONFIGURED: Still using placeholder value");
        }
        
        // Validate API key format - Google Gemini API keys start with "AIza"
        if (!apiKey.startsWith("AIza")) {
            logger.warn("⚠ Gemini API key format may be invalid - expected to start with 'AIza' but got '{}'", 
                       apiKey.substring(0, Math.min(4, apiKey.length())));
            logger.warn("  This may cause authentication failures with the Gemini API");
        } else {
            logger.info("✓ API key format validation passed");
        }
        
        logger.debug("Gemini API key validation completed");
    }
    
    private void validateModel() {
        String model = geminiProperties.getModel();
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini model is required but not configured");
        }
        
        // Validate against known Gemini models
        if (!model.startsWith("gemini-")) {
            logger.warn("Unknown Gemini model: {}. Expected models start with 'gemini-'", model);
        }
        
        logger.debug("Gemini model validation passed: {}", model);
    }
    
    private void validateBaseUrl() {
        String baseUrl = geminiProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini base URL is required but not configured");
        }
        
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Gemini base URL must use HTTPS");
        }
        
        if (!baseUrl.contains("generativelanguage.googleapis.com")) {
            logger.warn("Using non-standard Gemini base URL: {}", baseUrl);
        }
        
        logger.debug("Gemini base URL validation passed: {}", baseUrl);
    }
    
    private void validateParameters() {
        int maxTokens = geminiProperties.getMaxTokens();
        double temperature = geminiProperties.getTemperature();
        int timeoutMs = geminiProperties.getTimeoutMs();
        
        if (maxTokens < 1 || maxTokens > 8192) {
            throw new IllegalArgumentException("Max tokens must be between 1 and 8192, got: " + maxTokens);
        }
        
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0, got: " + temperature);
        }
        
        if (timeoutMs < 1000) {
            throw new IllegalArgumentException("Timeout must be at least 1000ms, got: " + timeoutMs);
        }
        
        // Warn about potentially problematic configurations
        if (maxTokens > 4000) {
            logger.warn("High max tokens configured ({}). This may result in expensive API calls", maxTokens);
        }
        
        if (temperature > 1.0) {
            logger.warn("High temperature configured ({}). This may result in unpredictable responses", temperature);
        }
        
        if (timeoutMs > 60000) {
            logger.warn("High timeout configured ({}ms). This may result in poor user experience", timeoutMs);
        }
        
        logger.debug("Gemini parameters validation passed");
    }
}