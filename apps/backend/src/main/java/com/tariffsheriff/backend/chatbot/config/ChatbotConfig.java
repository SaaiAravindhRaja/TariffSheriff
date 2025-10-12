package com.tariffsheriff.backend.chatbot.config;

import com.tariffsheriff.backend.chatbot.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

/**
 * Configuration class for chatbot-related beans and scheduled tasks
 */
@Configuration
@EnableScheduling
public class ChatbotConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotConfig.class);
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private ToolProperties toolProperties;
    
    /**
     * Validate tool configuration on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateToolConfiguration() {
        logger.info("Validating tool configuration...");
        
        try {
            // Validate global settings
            if (toolProperties.getDefaultTimeoutMs() < 1000) {
                throw new IllegalArgumentException("Default timeout must be at least 1000ms");
            }
            
            if (toolProperties.getMaxConcurrentExecutions() < 1) {
                throw new IllegalArgumentException("Max concurrent executions must be at least 1");
            }
            
            // Validate individual tool configurations
            for (Map.Entry<String, ToolProperties.ToolConfig> entry : toolProperties.getTools().entrySet()) {
                String toolName = entry.getKey();
                ToolProperties.ToolConfig config = entry.getValue();
                
                if (config.getTimeoutMs() < 1000) {
                    logger.warn("Tool {} has timeout less than 1000ms: {}ms", toolName, config.getTimeoutMs());
                }
                
                if (config.getMaxRetries() < 0) {
                    throw new IllegalArgumentException("Tool " + toolName + " has negative max retries");
                }
            }
            
            logger.info("Tool configuration validation completed successfully");
            logger.info("Configured {} tools with default timeout {}ms", 
                       toolProperties.getTools().size(), 
                       toolProperties.getDefaultTimeoutMs());
            
        } catch (Exception e) {
            logger.error("Tool configuration validation failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid tool configuration", e);
        }
    }
    
    /**
     * Scheduled task to clean up old rate limit entries
     * Runs every 15 minutes to prevent memory leaks
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    public void cleanupRateLimitEntries() {
        try {
            logger.debug("Starting scheduled cleanup of rate limit entries");
            rateLimitService.cleanupOldEntries();
            logger.debug("Completed scheduled cleanup of rate limit entries");
        } catch (Exception e) {
            logger.error("Error during scheduled rate limit cleanup", e);
        }
    }
}