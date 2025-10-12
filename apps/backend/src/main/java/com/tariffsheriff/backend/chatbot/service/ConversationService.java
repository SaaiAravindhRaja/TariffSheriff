package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.ai.context.UserContext;
import com.tariffsheriff.backend.ai.context.UserPreferences;
import com.tariffsheriff.backend.ai.context.ContextualEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for managing conversation history and persistence
 */
@Service
public class ConversationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    private static final int MAX_CONVERSATIONS_PER_USER = 50;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;
    
    // In-memory storage for conversations (in production, this would be database-backed)
    private final ConcurrentHashMap<String, Map<String, Conversation>> userConversations = new ConcurrentHashMap<>();
    
    // Enhanced storage for user preferences and behavior patterns
    private final ConcurrentHashMap<String, UserPreferences> userPreferences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserBehaviorProfile> userBehaviorProfiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ContextualEntity>> conversationEntities = new ConcurrentHashMap<>();
    
    /**
     * Store a message in conversation history with enhanced context management
     */
    public void storeMessage(String userId, String conversationId, ChatQueryRequest request, ChatQueryResponse response) {
        try {
            Map<String, Conversation> conversations = userConversations.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
            
            Conversation conversation = conversations.computeIfAbsent(conversationId, k -> new Conversation(conversationId, userId));
            
            // Extract and store entities from the query
            List<ContextualEntity> extractedEntities = extractEntitiesFromQuery(request.getQuery());
            updateConversationEntities(conversationId, extractedEntities);
            
            // Add user message with enhanced metadata
            ConversationMessage userMessage = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    "user",
                    request.getQuery(),
                    LocalDateTime.now(),
                    new MessageMetadata(null, null, extractedEntities, analyzeQueryIntent(request.getQuery()))
            );
            conversation.addMessage(userMessage);
            
            // Add assistant message with enhanced metadata
            ConversationMessage assistantMessage = new ConversationMessage(
                    UUID.randomUUID().toString(),
                    "assistant",
                    response.getResponse(),
                    LocalDateTime.now(),
                    new MessageMetadata(response.getToolsUsed(), response.getProcessingTimeMs(), 
                                      extractEntitiesFromResponse(response.getResponse()), null)
            );
            conversation.addMessage(assistantMessage);
            
            // Update user behavior profile
            updateUserBehaviorProfile(userId, request, response);
            
            // Update user preferences based on interaction
            updateUserPreferences(userId, request, response);
            
            // Limit conversation size
            conversation.limitMessages(MAX_MESSAGES_PER_CONVERSATION);
            
            // Limit number of conversations per user
            limitUserConversations(userId);
            
            logger.debug("Stored enhanced message for user {} in conversation {}", userId, conversationId);
            
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
     * Get conversation content as text for fallback analysis
     */
    public String getConversationContent(String conversationId, String userId) {
        try {
            Conversation conversation = getConversation(userId, conversationId);
            if (conversation == null || conversation.getMessages().isEmpty()) {
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            List<ConversationMessage> messages = conversation.getMessages();
            
            // Get the last few messages for context
            int startIndex = Math.max(0, messages.size() - 6); // Last 6 messages
            
            for (int i = startIndex; i < messages.size(); i++) {
                ConversationMessage message = messages.get(i);
                content.append(message.getRole()).append(": ")
                       .append(message.getContent()).append("\n\n");
            }
            
            return content.toString().trim();
            
        } catch (Exception e) {
            logger.warn("Error getting conversation content for conversation {}", conversationId, e);
            return null;
        }
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
     * Get user preferences for personalized responses
     */
    public UserPreferences getUserPreferences(String userId) {
        return userPreferences.computeIfAbsent(userId, k -> new UserPreferences());
    }
    
    /**
     * Update user preferences based on interaction patterns
     */
    public void updateUserPreferences(String userId, ChatQueryRequest request, ChatQueryResponse response) {
        try {
            UserPreferences prefs = getUserPreferences(userId);
            
            // Analyze query patterns to infer preferences
            String query = request.getQuery().toLowerCase();
            
            // Detect preferred countries
            List<String> mentionedCountries = extractCountries(query);
            for (String country : mentionedCountries) {
                prefs.incrementCountryPreference(country);
            }
            
            // Detect preferred product categories
            List<String> mentionedProducts = extractProducts(query);
            for (String product : mentionedProducts) {
                prefs.incrementProductPreference(product);
            }
            
            // Detect response format preferences
            if (response.getResponse().contains("table") || query.contains("compare")) {
                prefs.incrementFormatPreference("tabular");
            }
            if (query.contains("chart") || query.contains("graph")) {
                prefs.incrementFormatPreference("visual");
            }
            
            // Update complexity preference based on query sophistication
            if (isComplexQuery(query)) {
                prefs.incrementComplexityPreference("detailed");
            } else {
                prefs.incrementComplexityPreference("simple");
            }
            
            logger.debug("Updated preferences for user {}", userId);
            
        } catch (Exception e) {
            logger.warn("Error updating user preferences for user {}", userId, e);
        }
    }
    
    /**
     * Get user behavior profile for personalized responses
     */
    public UserBehaviorProfile getUserBehaviorProfile(String userId) {
        return userBehaviorProfiles.computeIfAbsent(userId, k -> new UserBehaviorProfile(userId));
    }
    
    /**
     * Update user behavior profile based on interactions
     */
    private void updateUserBehaviorProfile(String userId, ChatQueryRequest request, ChatQueryResponse response) {
        try {
            UserBehaviorProfile profile = getUserBehaviorProfile(userId);
            
            profile.recordQuery(request.getQuery());
            profile.recordResponseTime(response.getProcessingTimeMs());
            profile.recordToolsUsed(response.getToolsUsed());
            
            // Analyze query patterns
            String query = request.getQuery().toLowerCase();
            if (query.contains("urgent") || query.contains("asap") || query.contains("quickly")) {
                profile.incrementUrgencyPattern();
            }
            
            if (query.contains("compare") || query.contains("vs") || query.contains("versus")) {
                profile.incrementComparisonPattern();
            }
            
            if (query.contains("analyze") || query.contains("assessment") || query.contains("evaluation")) {
                profile.incrementAnalysisPattern();
            }
            
            logger.debug("Updated behavior profile for user {}", userId);
            
        } catch (Exception e) {
            logger.warn("Error updating user behavior profile for user {}", userId, e);
        }
    }
    
    /**
     * Resolve contextual references in queries (e.g., "the Germany option we discussed")
     */
    public String resolveContextualReferences(String query, String conversationId, String userId) {
        try {
            // Get conversation entities for context
            List<ContextualEntity> entities = conversationEntities.getOrDefault(conversationId, new ArrayList<>());
            
            // Pattern matching for common contextual references
            String resolvedQuery = query;
            
            // Resolve "the [country] option"
            Pattern countryOptionPattern = Pattern.compile("the ([a-zA-Z]+) option", Pattern.CASE_INSENSITIVE);
            Matcher matcher = countryOptionPattern.matcher(resolvedQuery);
            if (matcher.find()) {
                String country = matcher.group(1);
                // Find the most recent mention of this country in context
                Optional<ContextualEntity> countryEntity = entities.stream()
                        .filter(e -> "COUNTRY".equals(e.getType()) && 
                                    e.getValue().toLowerCase().contains(country.toLowerCase()))
                        .max(Comparator.comparing(ContextualEntity::getLastMentioned));
                
                if (countryEntity.isPresent()) {
                    resolvedQuery = resolvedQuery.replace(matcher.group(0), 
                            "the " + countryEntity.get().getValue() + " option");
                }
            }
            
            // Resolve "that product" or "this item"
            Pattern productPattern = Pattern.compile("(that product|this item|it)", Pattern.CASE_INSENSITIVE);
            matcher = productPattern.matcher(resolvedQuery);
            if (matcher.find()) {
                Optional<ContextualEntity> productEntity = entities.stream()
                        .filter(e -> "PRODUCT".equals(e.getType()))
                        .max(Comparator.comparing(ContextualEntity::getLastMentioned));
                
                if (productEntity.isPresent()) {
                    resolvedQuery = resolvedQuery.replace(matcher.group(0), productEntity.get().getValue());
                }
            }
            
            // Resolve "the previous calculation" or "that analysis"
            Pattern analysisPattern = Pattern.compile("(the previous calculation|that analysis|the last result)", Pattern.CASE_INSENSITIVE);
            if (analysisPattern.matcher(resolvedQuery).find()) {
                // Add context about what was previously calculated
                Conversation conversation = getConversation(userId, conversationId);
                if (conversation != null) {
                    Optional<ConversationMessage> lastAnalysis = conversation.getMessages().stream()
                            .filter(msg -> "assistant".equals(msg.getRole()) && 
                                         (msg.getContent().contains("calculation") || 
                                          msg.getContent().contains("analysis") ||
                                          msg.getContent().contains("tariff")))
                            .reduce((first, second) -> second);
                    
                    if (lastAnalysis.isPresent()) {
                        resolvedQuery += " (referring to: " + 
                                       lastAnalysis.get().getContent().substring(0, 
                                       Math.min(100, lastAnalysis.get().getContent().length())) + "...)";
                    }
                }
            }
            
            if (!resolvedQuery.equals(query)) {
                logger.debug("Resolved contextual references: '{}' -> '{}'", query, resolvedQuery);
            }
            
            return resolvedQuery;
            
        } catch (Exception e) {
            logger.warn("Error resolving contextual references", e);
            return query; // Return original query if resolution fails
        }
    }
    
    /**
     * Generate conversation summary with key insights
     */
    public String generateConversationSummary(String userId, String conversationId) {
        try {
            Conversation conversation = getConversation(userId, conversationId);
            if (conversation == null || conversation.getMessages().isEmpty()) {
                return "No conversation found.";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("Conversation Summary:\n\n");
            
            // Extract key topics discussed
            Set<String> topics = new HashSet<>();
            Set<String> countries = new HashSet<>();
            Set<String> products = new HashSet<>();
            
            for (ConversationMessage message : conversation.getMessages()) {
                if ("user".equals(message.getRole())) {
                    String content = message.getContent().toLowerCase();
                    
                    // Extract topics
                    if (content.contains("tariff")) topics.add("tariffs");
                    if (content.contains("trade agreement")) topics.add("trade agreements");
                    if (content.contains("hs code")) topics.add("HS codes");
                    if (content.contains("compliance")) topics.add("compliance");
                    if (content.contains("risk")) topics.add("risk assessment");
                    
                    // Extract countries and products
                    countries.addAll(extractCountries(content));
                    products.addAll(extractProducts(content));
                }
            }
            
            if (!topics.isEmpty()) {
                summary.append("Topics discussed: ").append(String.join(", ", topics)).append("\n");
            }
            if (!countries.isEmpty()) {
                summary.append("Countries mentioned: ").append(String.join(", ", countries)).append("\n");
            }
            if (!products.isEmpty()) {
                summary.append("Products discussed: ").append(String.join(", ", products)).append("\n");
            }
            
            summary.append("\nTotal messages: ").append(conversation.getMessages().size());
            summary.append("\nConversation started: ").append(conversation.getCreatedAt());
            
            return summary.toString();
            
        } catch (Exception e) {
            logger.error("Error generating conversation summary", e);
            return "Error generating summary.";
        }
    }
    
    /**
     * Extract key entities from conversation for context
     */
    public List<ContextualEntity> extractConversationEntities(String userId, String conversationId) {
        return conversationEntities.getOrDefault(conversationId, new ArrayList<>());
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
     * Extract entities from query text
     */
    private List<ContextualEntity> extractEntitiesFromQuery(String query) {
        List<ContextualEntity> entities = new ArrayList<>();
        
        try {
            // Extract countries
            List<String> countries = extractCountries(query);
            for (String country : countries) {
                entities.add(new ContextualEntity("COUNTRY", country, LocalDateTime.now(), 1));
            }
            
            // Extract products
            List<String> products = extractProducts(query);
            for (String product : products) {
                entities.add(new ContextualEntity("PRODUCT", product, LocalDateTime.now(), 1));
            }
            
            // Extract HS codes
            Pattern hsCodePattern = Pattern.compile("\\b\\d{4,10}\\b");
            Matcher matcher = hsCodePattern.matcher(query);
            while (matcher.find()) {
                entities.add(new ContextualEntity("HS_CODE", matcher.group(), LocalDateTime.now(), 1));
            }
            
            // Extract monetary amounts
            Pattern moneyPattern = Pattern.compile("\\$[\\d,]+(?:\\.\\d{2})?");
            matcher = moneyPattern.matcher(query);
            while (matcher.find()) {
                entities.add(new ContextualEntity("AMOUNT", matcher.group(), LocalDateTime.now(), 1));
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting entities from query", e);
        }
        
        return entities;
    }
    
    /**
     * Extract entities from response text
     */
    private List<ContextualEntity> extractEntitiesFromResponse(String response) {
        List<ContextualEntity> entities = new ArrayList<>();
        
        try {
            // Extract countries from response
            List<String> countries = extractCountries(response);
            for (String country : countries) {
                entities.add(new ContextualEntity("COUNTRY", country, LocalDateTime.now(), 1));
            }
            
            // Extract tariff rates
            Pattern tariffPattern = Pattern.compile("(\\d+(?:\\.\\d+)?%)");
            Matcher matcher = tariffPattern.matcher(response);
            while (matcher.find()) {
                entities.add(new ContextualEntity("TARIFF_RATE", matcher.group(), LocalDateTime.now(), 1));
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting entities from response", e);
        }
        
        return entities;
    }
    
    /**
     * Update conversation entities with new entities
     */
    private void updateConversationEntities(String conversationId, List<ContextualEntity> newEntities) {
        List<ContextualEntity> existingEntities = conversationEntities.computeIfAbsent(conversationId, k -> new ArrayList<>());
        
        for (ContextualEntity newEntity : newEntities) {
            // Check if entity already exists
            Optional<ContextualEntity> existing = existingEntities.stream()
                    .filter(e -> e.getType().equals(newEntity.getType()) && 
                                e.getValue().equalsIgnoreCase(newEntity.getValue()))
                    .findFirst();
            
            if (existing.isPresent()) {
                // Update existing entity
                existing.get().incrementMentionCount();
                existing.get().updateLastMentioned(LocalDateTime.now());
            } else {
                // Add new entity
                existingEntities.add(newEntity);
            }
        }
        
        // Limit entities per conversation
        if (existingEntities.size() > 100) {
            existingEntities.sort(Comparator.comparing(ContextualEntity::getLastMentioned).reversed());
            existingEntities.subList(100, existingEntities.size()).clear();
        }
    }
    
    /**
     * Extract countries from text
     */
    private List<String> extractCountries(String text) {
        List<String> countries = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Common countries in trade context
        String[] commonCountries = {
            "united states", "usa", "us", "america",
            "china", "germany", "japan", "canada", "mexico",
            "united kingdom", "uk", "britain", "france", "italy",
            "south korea", "korea", "india", "brazil", "australia",
            "netherlands", "spain", "belgium", "singapore", "taiwan"
        };
        
        for (String country : commonCountries) {
            if (lowerText.contains(country)) {
                countries.add(country);
            }
        }
        
        return countries;
    }
    
    /**
     * Extract products from text
     */
    private List<String> extractProducts(String text) {
        List<String> products = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // Common product categories in trade
        String[] commonProducts = {
            "electronics", "automotive", "textiles", "machinery", "chemicals",
            "food", "beverages", "steel", "aluminum", "plastic", "wood",
            "pharmaceuticals", "medical devices", "computers", "smartphones",
            "cars", "vehicles", "clothing", "furniture", "toys"
        };
        
        for (String product : commonProducts) {
            if (lowerText.contains(product)) {
                products.add(product);
            }
        }
        
        return products;
    }
    
    /**
     * Analyze query intent
     */
    private String analyzeQueryIntent(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("tariff") || lowerQuery.contains("duty") || lowerQuery.contains("rate")) {
            return "TARIFF_INQUIRY";
        } else if (lowerQuery.contains("hs code") || lowerQuery.contains("classification")) {
            return "CLASSIFICATION_INQUIRY";
        } else if (lowerQuery.contains("agreement") || lowerQuery.contains("fta")) {
            return "AGREEMENT_INQUIRY";
        } else if (lowerQuery.contains("compare") || lowerQuery.contains("vs")) {
            return "COMPARISON_REQUEST";
        } else if (lowerQuery.contains("compliance") || lowerQuery.contains("regulation")) {
            return "COMPLIANCE_INQUIRY";
        } else if (lowerQuery.contains("risk") || lowerQuery.contains("assessment")) {
            return "RISK_INQUIRY";
        } else {
            return "GENERAL_INQUIRY";
        }
    }
    
    /**
     * Check if query is complex
     */
    private boolean isComplexQuery(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Count complexity indicators
        int complexityScore = 0;
        
        if (lowerQuery.contains("compare") || lowerQuery.contains("vs")) complexityScore++;
        if (lowerQuery.contains("analyze") || lowerQuery.contains("analysis")) complexityScore++;
        if (lowerQuery.contains("calculate") || lowerQuery.contains("computation")) complexityScore++;
        if (lowerQuery.contains("scenario") || lowerQuery.contains("what if")) complexityScore++;
        if (lowerQuery.contains("optimize") || lowerQuery.contains("best")) complexityScore++;
        if (lowerQuery.split("\\s+").length > 15) complexityScore++; // Long queries
        if (lowerQuery.split("and|or").length > 2) complexityScore++; // Multiple conditions
        
        return complexityScore >= 2;
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
        
        public void limitMessages(int maxMessages) {
            if (messages.size() > maxMessages) {
                // Keep the most recent messages
                List<ConversationMessage> recentMessages = messages.subList(
                        messages.size() - maxMessages, messages.size());
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
        private final MessageMetadata metadata;
        
        public ConversationMessage(String id, String role, String content, 
                                 LocalDateTime timestamp, MessageMetadata metadata) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }
        
        // Getters
        public String getId() { return id; }
        public String getRole() { return role; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public MessageMetadata getMetadata() { return metadata; }
    }
    
    /**
     * Enhanced message metadata
     */
    public static class MessageMetadata {
        private final List<String> toolsUsed;
        private final Long processingTimeMs;
        private final List<ContextualEntity> entities;
        private final String intent;
        
        public MessageMetadata(List<String> toolsUsed, Long processingTimeMs, 
                             List<ContextualEntity> entities, String intent) {
            this.toolsUsed = toolsUsed != null ? new ArrayList<>(toolsUsed) : Collections.emptyList();
            this.processingTimeMs = processingTimeMs;
            this.entities = entities != null ? new ArrayList<>(entities) : Collections.emptyList();
            this.intent = intent;
        }
        
        // Getters
        public List<String> getToolsUsed() { return new ArrayList<>(toolsUsed); }
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public List<ContextualEntity> getEntities() { return new ArrayList<>(entities); }
        public String getIntent() { return intent; }
    }
    
    /**
     * Conversation summary for listing
     */
    public static class ConversationSummary {
        private final String id;
        private final String title;
        private final LocalDateTime lastMessageTime;
        private final int messageCount;
        private final String lastMessage;
        
        public ConversationSummary(String id, String title, LocalDateTime lastMessageTime, int messageCount) {
            this.id = id;
            this.title = title;
            this.lastMessageTime = lastMessageTime;
            this.messageCount = messageCount;
            this.lastMessage = title; // Use title as last message for now
        }
        
        public ConversationSummary(String id, String title, LocalDateTime lastMessageTime, int messageCount, String lastMessage) {
            this.id = id;
            this.title = title;
            this.lastMessageTime = lastMessageTime;
            this.messageCount = messageCount;
            this.lastMessage = lastMessage;
        }
        
        // Getters
        public String getId() { return id; }
        public String getConversationId() { return id; } // Alias for backward compatibility
        public String getTitle() { return title; }
        public LocalDateTime getLastMessageTime() { return lastMessageTime; }
        public int getMessageCount() { return messageCount; }
        public String getLastMessage() { return lastMessage; }
    }
    
    /**
     * Conversation statistics
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
    
    /**
     * User behavior profile for personalized responses
     */
    public static class UserBehaviorProfile {
        private final String userId;
        private final LocalDateTime createdAt;
        private final List<String> queryHistory;
        private final Map<String, Integer> queryPatterns;
        private final Map<String, Integer> toolUsagePatterns;
        private final List<Long> responseTimeHistory;
        private int urgencyPatternCount;
        private int comparisonPatternCount;
        private int analysisPatternCount;
        
        public UserBehaviorProfile(String userId) {
            this.userId = userId;
            this.createdAt = LocalDateTime.now();
            this.queryHistory = new ArrayList<>();
            this.queryPatterns = new HashMap<>();
            this.toolUsagePatterns = new HashMap<>();
            this.responseTimeHistory = new ArrayList<>();
            this.urgencyPatternCount = 0;
            this.comparisonPatternCount = 0;
            this.analysisPatternCount = 0;
        }
        
        public void recordQuery(String query) {
            queryHistory.add(query);
            if (queryHistory.size() > 100) { // Limit history size
                queryHistory.remove(0);
            }
        }
        
        public void recordResponseTime(Long responseTime) {
            if (responseTime != null) {
                responseTimeHistory.add(responseTime);
                if (responseTimeHistory.size() > 50) { // Limit history size
                    responseTimeHistory.remove(0);
                }
            }
        }
        
        public void recordToolsUsed(List<String> tools) {
            if (tools != null) {
                for (String tool : tools) {
                    toolUsagePatterns.merge(tool, 1, Integer::sum);
                }
            }
        }
        
        public void incrementUrgencyPattern() { urgencyPatternCount++; }
        public void incrementComparisonPattern() { comparisonPatternCount++; }
        public void incrementAnalysisPattern() { analysisPatternCount++; }
        
        // Getters
        public String getUserId() { return userId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<String> getQueryHistory() { return new ArrayList<>(queryHistory); }
        public Map<String, Integer> getQueryPatterns() { return new HashMap<>(queryPatterns); }
        public Map<String, Integer> getToolUsagePatterns() { return new HashMap<>(toolUsagePatterns); }
        public List<Long> getResponseTimeHistory() { return new ArrayList<>(responseTimeHistory); }
        public int getUrgencyPatternCount() { return urgencyPatternCount; }
        public int getComparisonPatternCount() { return comparisonPatternCount; }
        public int getAnalysisPatternCount() { return analysisPatternCount; }
        
        public double getAverageResponseTime() {
            return responseTimeHistory.isEmpty() ? 0.0 : 
                   responseTimeHistory.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        
        public String getMostUsedTool() {
            return toolUsagePatterns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
        }
        
        public boolean prefersComparisons() {
            return comparisonPatternCount > analysisPatternCount && comparisonPatternCount > urgencyPatternCount;
        }
        
        public boolean prefersDetailedAnalysis() {
            return analysisPatternCount > comparisonPatternCount && analysisPatternCount > urgencyPatternCount;
        }
        
        public boolean hasUrgencyPattern() {
            return urgencyPatternCount > 0;
        }
    }
}