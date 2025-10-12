package com.tariffsheriff.backend.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Configuration properties for Gemini API integration
 */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    
    @NotBlank(message = "Gemini API key is required")
    private String apiKey;
    
    @NotBlank(message = "Gemini model is required")
    private String model = "gemini-1.5-pro";
    
    @NotBlank(message = "Gemini base URL is required")
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 8192, message = "Max tokens cannot exceed 8192")
    private int maxTokens = 4000;
    
    @Min(value = 0, message = "Temperature must be at least 0")
    @Max(value = 2, message = "Temperature cannot exceed 2")
    private double temperature = 0.7;
    
    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    private int timeoutMs = 30000;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}