package com.tariffsheriff.backend.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
     * Validates Gemini configuration after application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Validating Gemini configuration...");
        
        try {
            validateApiKey();
            validateModel();
            validateBaseUrl();
            validateParameters();
            
            logger.info("Gemini configuration validation completed successfully");
            logger.info("Using Gemini model: {}", geminiProperties.getModel());
            logger.info("Max tokens: {}, Temperature: {}, Timeout: {}ms", 
                       geminiProperties.getMaxTokens(), 
                       geminiProperties.getTemperature(), 
                       geminiProperties.getTimeoutMs());
            
        } catch (Exception e) {
            logger.error("Gemini configuration validation failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid Gemini configuration", e);
        }
    }
    
    private void validateApiKey() {
        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key is required but not configured");
        }
        
        if ("your_api_key_here".equals(apiKey)) {
            throw new IllegalArgumentException("Gemini API key is not properly configured (still using placeholder value)");
        }
        
        // Basic format validation - Gemini API keys typically start with "AI"
        if (!apiKey.startsWith("AI")) {
            logger.warn("Gemini API key format may be invalid - expected to start with 'AI'");
        }
        
        logger.debug("Gemini API key validation passed");
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