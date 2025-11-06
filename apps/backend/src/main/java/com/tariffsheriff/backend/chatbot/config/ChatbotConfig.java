package com.tariffsheriff.backend.chatbot.config;

import com.tariffsheriff.backend.chatbot.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Simplified configuration for chatbot scheduled tasks
 */
@Configuration
@EnableScheduling
public class ChatbotConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotConfig.class);
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Scheduled task to clean up inactive rate limiters
     * Runs every 15 minutes to prevent memory buildup
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
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
