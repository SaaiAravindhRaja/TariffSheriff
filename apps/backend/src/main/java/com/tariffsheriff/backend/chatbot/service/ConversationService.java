package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simplified service for managing conversation history
 */
@Service
public class ConversationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    private static final int MAX_CONVERSATIONS_PER_USER = 50;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;
    
    // In-memory storage for conversations (in production, this would be database-backed)
    private final ConcurrentHashMap<String, Map<String, Conversation>> userConversations = new ConcurrentHashMap<>();
    
    /**
     * Store a message in conversation history
     */
    public void storeMessage(String userId, String conversationId, ChatQueryRequest request, ChatQueryResponse response) {
        try {
            Map<String, Conversation> conversations = userConversations.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
            
            Conversation conversation = conversations.computeIfAbsent(conversationId, k -> new Conversation(conversationId, userId));
            
            // Add user message
            ConversationMessage userMessage = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    "user",
                    request.getQuery(),
                    LocalDateTime.now()
            );
            conversation.addMessage(userMessage);
            
            // Add assistant message
            ConversationMessage assistantMessage = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    "assistant",
                    response.getResponse(),
                    LocalDateTime.now()
            );
            conversation.addMessage(assistantMessage);
            
            // Limit conversation size
            conversation.limitMessages(MAX_MESSAGES_PER_CONVERSATION);
            
            // Limit number of conversations per user
            limitUserConversations(userId);
            
            logger.debug("Stored message for user {} in conversation {}", userId, conversationId);
            
        } catch (Exception e) {
            logger.error("Error storing conversation message for user {} conversation {}", userId, conversationId, e);
        }
    }
    
    /**
     * Get conversation history for a user
     */
    public List<ConversationSummary> getUserConversations(String userId) {
        Map<String, Conversation> conversations = userConversations.get(userId);
        if (conversations == null) {
            return Collections.emptyList();
        }
        
        return conversations.values().stream()
                .sorted((c1, c2) -> c2.getLastMessageTime().compareTo(c1.getLastMessageTime()))
                .map(this::createConversationSummary)
                .collect(Collectors.toList());
    }
    
    /**
     * Get specific conversation
     */
    public Conversation getConversation(String userId, String conversationId) {
        Map<String, Conversation> conversations = userConversations.get(userId);
        if (conversations == null) {
            return null;
        }
        
        return conversations.get(conversationId);
    }
    
    /**
     * Delete a conversation
     */
    public boolean deleteConversation(String userId, String conversationId) {
        Map<String, Conversation> conversations = userConversations.get(userId);
        if (conversations == null) {
            return false;
        }
        
        boolean removed = conversations.remove(conversationId) != null;
        if (removed) {
            logger.info("Deleted conversation {} for user {}", conversationId, userId);
        }
        
        return removed;
    }
    
    /**
     * Clear all conversations for a user
     */
    public void clearUserConversations(String userId) {
        userConversations.remove(userId);
        logger.info("Cleared all conversations for user {}", userId);
    }
    
    /**
     * Get conversation statistics
     */
    public ConversationStats getStats() {
        int totalUsers = userConversations.size();
        int totalConversations = userConversations.values().stream()
                .mapToInt(Map::size)
                .sum();
        int totalMessages = userConversations.values().stream()
                .flatMap(conversations -> conversations.values().stream())
                .mapToInt(conversation -> conversation.getMessages().size())
                .sum();
        
        return new ConversationStats(totalUsers, totalConversations, totalMessages);
    }
    
    /**
     * Limit number of conversations per user
     */
    private void limitUserConversations(String userId) {
        Map<String, Conversation> conversations = userConversations.get(userId);
        if (conversations == null || conversations.size() <= MAX_CONVERSATIONS_PER_USER) {
            return;
        }
        
        // Remove oldest conversations
        List<String> conversationIds = conversations.values().stream()
                .sorted(Comparator.comparing(Conversation::getLastMessageTime))
                .limit(conversations.size() - MAX_CONVERSATIONS_PER_USER)
                .map(Conversation::getId)
                .collect(Collectors.toList());
        
        conversationIds.forEach(conversations::remove);
        
        logger.debug("Limited conversations for user {} to {}", userId, MAX_CONVERSATIONS_PER_USER);
    }
    
    /**
     * Create conversation summary
     */
    private ConversationSummary createConversationSummary(Conversation conversation) {
        String title = generateConversationTitle(conversation);
        String lastMessage = conversation.getMessages().stream()
                .reduce((first, second) -> second)
                .map(ConversationMessage::getContent)
                .orElse("");
        
        return new ConversationSummary(
                conversation.getId(),
                title,
                conversation.getLastMessageTime(),
                conversation.getMessages().size(),
                lastMessage
        );
    }
    
    /**
     * Generate conversation title from first user message
     */
    private String generateConversationTitle(Conversation conversation) {
        return conversation.getMessages().stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .findFirst()
                .map(msg -> {
                    String content = msg.getContent();
                    if (content.length() > 50) {
                        return content.substring(0, 47) + "...";
                    }
                    return content;
                })
                .orElse("New Conversation");
    }
    
    /**
     * Conversation data model
     */
    public static class Conversation {
        private final String id;
        private final String userId;
        private final LocalDateTime createdAt;
        private final List<ConversationMessage> messages;
        
        public Conversation(String id, String userId) {
            this.id = id;
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.messages = new ArrayList<>();
        }
        
        public void addMessage(ConversationMessage message) {
            messages.add(message);
        }
        
       public void limitMessages(int maxMessagesCount) {
            if (messages.size() > maxMessagesCount) {
                // Keep the most recent messages
                // e.g., size is 101, max is 100. We want sublist from index 1 to 101.
                List<ConversationMessage> recentMessages = messages.subList(
                        messages.size() - maxMessagesCount, messages.size());
                messages.clear();
                messages.addAll(recentMessages);
            }
        }
        
        public LocalDateTime getLastMessageTime() {
            return messages.isEmpty() ? createdAt : 
                    messages.get(messages.size() - 1).getTimestamp();
        }
        
        // Getters
        public String getId() { return id; }
        public String getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<ConversationMessage> getMessages() { return new ArrayList<>(messages); }
    }
    
    /**
     * Conversation message data model
     */
    public static class ConversationMessage {
        private final String id;
        private final String role;
        private final String content;
        private final LocalDateTime timestamp;
        
        public ConversationMessage(String id, String role, String content, LocalDateTime timestamp) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getId() { return id; }
        public String getRole() { return role; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Conversation summary data model
     */
    public static class ConversationSummary {
        private final String id;
        private final String title;
        private final LocalDateTime lastMessageTime;
        private final int messageCount;
        private final String lastMessage;
        
        public ConversationSummary(String id, String title, LocalDateTime lastMessageTime, 
                                 int messageCount, String lastMessage) {
            this.id = id;
            this.title = title;
            this.lastMessageTime = lastMessageTime;
            this.messageCount = messageCount;
            this.lastMessage = lastMessage;
        }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public LocalDateTime getLastMessageTime() { return lastMessageTime; }
        public int getMessageCount() { return messageCount; }
        public String getLastMessage() { return lastMessage; }
    }
    
    /**
     * Conversation statistics data model
     */
    public static class ConversationStats {
        private final int totalUsers;
        private final int totalConversations;
        private final int totalMessages;
        
        public ConversationStats(int totalUsers, int totalConversations, int totalMessages) {
            this.totalUsers = totalUsers;
            this.totalConversations = totalConversations;
            this.totalMessages = totalMessages;
        }
        
        // Getters
        public int getTotalUsers() { return totalUsers; }
        public int getTotalConversations() { return totalConversations; }
        public int getTotalMessages() { return totalMessages; }
    }
}
