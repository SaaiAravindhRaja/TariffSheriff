package com.tariffsheriff.backend.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Configuration properties for OpenAI API integration
 */
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    
    @NotBlank(message = "OpenAI API key is required")
    private String apiKey;
    
    @NotBlank(message = "OpenAI model is required")
    private String model = "gpt-4o-mini";
    
    @NotBlank(message = "OpenAI base URL is required")
    private String baseUrl = "https://api.openai.com/v1";
    
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 16384, message = "Max tokens cannot exceed 16384")
    private int maxTokens = 1000;
    
    @Min(value = 0, message = "Temperature must be at least 0")
    @Max(value = 2, message = "Temperature cannot exceed 2")
    private double temperature = 0.7;
    
    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    private int timeoutMs = 30000;
    
    @Min(value = 0, message = "Max retries must be at least 0")
    @Max(value = 5, message = "Max retries cannot exceed 5")
    private int maxRetries = 2;
    
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
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
