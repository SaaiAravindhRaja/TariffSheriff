package com.tariffsheriff.backend.ai.context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User context containing preferences and conversation history
 */
public class UserContext {
    private final String userId;
    private final UserPreferences preferences;
    private final List<ConversationSummary> conversationHistory;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    
    public UserContext(String userId, UserPreferences preferences, 
                      List<ConversationSummary> conversationHistory, LocalDateTime createdAt) {
        this.userId = userId;
        this.preferences = preferences;
        this.conversationHistory = new ArrayList<>(conversationHistory);
        this.createdAt = createdAt;
        this.lastUpdated = createdAt;
    }
    
    /**
     * Add conversation summary to history
     */
    public void addConversationSummary(ConversationSummary summary) {
        conversationHistory.add(summary);
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Limit conversation history size
     */
    public void limitConversationHistory(int maxSize) {
        if (conversationHistory.size() > maxSize) {
            // Keep most recent conversations
            List<ConversationSummary> recent = conversationHistory.subList(
                conversationHistory.size() - maxSize, conversationHistory.size());
            conversationHistory.clear();
            conversationHistory.addAll(recent);
        }
        lastUpdated = LocalDateTime.now();
    }
    
    // Getters
    public String getUserId() { return userId; }
    public UserPreferences getPreferences() { return preferences; }
    public List<ConversationSummary> getConversationHistory() { return new ArrayList<>(conversationHistory); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    /**
     * Conversation summary for user context
     */
    public static class ConversationSummary {
        private final String conversationId;
        private final String topic;
        private final List<String> keyEntities;
        private final LocalDateTime timestamp;
        
        public ConversationSummary(String conversationId, String topic, 
                                 List<String> keyEntities, LocalDateTime timestamp) {
            this.conversationId = conversationId;
            this.topic = topic;
            this.keyEntities = new ArrayList<>(keyEntities);
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getConversationId() { return conversationId; }
        public String getTopic() { return topic; }
        public List<String> getKeyEntities() { return new ArrayList<>(keyEntities); }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}