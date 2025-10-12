package com.tariffsheriff.backend.ai.context;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages conversation context, user preferences, and contextual reference resolution
 */
@Service
public class ContextManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextManager.class);
    
    private static final int MAX_CONTEXT_HISTORY = 10;
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\b(the|that|this)\\s+(\\w+(?:\\s+\\w+){0,2})\\s+(?:we|I)\\s+(?:discussed|mentioned|talked about|looked at)\\b", Pattern.CASE_INSENSITIVE);
    
    private final ConversationService conversationService;
    
    // In-memory storage for user contexts (would be database-backed in production)
    private final ConcurrentHashMap<String, UserContext> userContexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, QueryContext> activeQueryContexts = new ConcurrentHashMap<>();
    
    @Autowired
    public ContextManager(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    /**
     * Load user context including preferences and conversation history
     */
    public UserContext loadUserContext(String userId) {
        if (userId == null) {
            return createDefaultUserContext();
        }
        
        return userContexts.computeIfAbsent(userId, this::createUserContext);
    }
    
    /**
     * Build query context from request and user context
     */
    public QueryContext buildQueryContext(ChatQueryRequest request, UserContext userContext) {
        try {
            String conversationId = request.getConversationId();
            
            // Get conversation history
            List<ContextualMessage> conversationHistory = loadConversationHistory(
                userContext.getUserId(), conversationId);
            
            // Extract referenced entities from query
            List<ContextualEntity> referencedEntities = extractReferences(
                request.getQuery(), conversationHistory);
            
            // Create query context
            QueryContext queryContext = new QueryContext(
                request.getQuery(),
                conversationId,
                userContext,
                conversationHistory,
                referencedEntities,
                LocalDateTime.now()
            );
            
            // Store active query context
            activeQueryContexts.put(conversationId, queryContext);
            
            logger.debug("Built query context for conversation {} with {} history messages and {} references", 
                    conversationId, conversationHistory.size(), referencedEntities.size());
            
            return queryContext;
            
        } catch (Exception e) {
            logger.error("Error building query context for user {}", userContext.getUserId(), e);
            
            // Return minimal context on error
            return new QueryContext(
                request.getQuery(),
                request.getConversationId(),
                userContext,
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDateTime.now()
            );
        }
    }
    
    /**
     * Update context with new information from query and response
     */
    public void updateContext(String conversationId, QueryContext queryContext, 
                             ChatQueryRequest request, String response) {
        try {
            // Update user preferences based on query patterns
            updateUserPreferences(queryContext.getUserContext(), request.getQuery());
            
            // Extract and store entities mentioned in the conversation
            List<ContextualEntity> newEntities = extractEntitiesFromResponse(response);
            queryContext.addEntities(newEntities);
            
            // Update conversation summary
            updateConversationSummary(conversationId, request.getQuery(), response);
            
            logger.debug("Updated context for conversation {} with {} new entities", 
                    conversationId, newEntities.size());
            
        } catch (Exception e) {
            logger.error("Error updating context for conversation {}", conversationId, e);
        }
    }
    
    /**
     * Extract contextual references from query
     */
    public List<ContextualEntity> extractReferences(String query, List<ContextualMessage> history) {
        List<ContextualEntity> references = new ArrayList<>();
        
        try {
            Matcher matcher = REFERENCE_PATTERN.matcher(query);
            
            while (matcher.find()) {
                String referenceType = matcher.group(1); // "the", "that", "this"
                String referenceObject = matcher.group(2); // the referenced object
                
                // Find matching entities in conversation history
                ContextualEntity matchedEntity = findMatchingEntity(referenceObject, history);
                if (matchedEntity != null) {
                    references.add(matchedEntity);
                    logger.debug("Resolved reference '{}' to entity: {}", 
                            matcher.group(), matchedEntity.getValue());
                }
            }
            
            // Also look for direct country/product references
            references.addAll(extractDirectReferences(query, history));
            
        } catch (Exception e) {
            logger.error("Error extracting references from query", e);
        }
        
        return references;
    }
    
    /**
     * Maintain memory with configurable retention policies
     */
    public void maintainMemory(String userId, int maxMessages) {
        try {
            UserContext userContext = userContexts.get(userId);
            if (userContext == null) {
                return;
            }
            
            // Clean up old query contexts
            activeQueryContexts.entrySet().removeIf(entry -> {
                QueryContext context = entry.getValue();
                return context.getTimestamp().isBefore(LocalDateTime.now().minusHours(24));
            });
            
            // Update user context retention
            userContext.limitConversationHistory(maxMessages);
            
            logger.debug("Maintained memory for user {} with max {} messages", userId, maxMessages);
            
        } catch (Exception e) {
            logger.error("Error maintaining memory for user {}", userId, e);
        }
    }
    
    /**
     * Create user context for new user
     */
    private UserContext createUserContext(String userId) {
        UserPreferences preferences = new UserPreferences();
        return new UserContext(userId, preferences, new ArrayList<>(), LocalDateTime.now());
    }
    
    /**
     * Create default user context for anonymous users
     */
    private UserContext createDefaultUserContext() {
        UserPreferences preferences = new UserPreferences();
        return new UserContext("anonymous", preferences, new ArrayList<>(), LocalDateTime.now());
    }
    
    /**
     * Load conversation history as contextual messages
     */
    private List<ContextualMessage> loadConversationHistory(String userId, String conversationId) {
        List<ContextualMessage> contextualMessages = new ArrayList<>();
        
        try {
            if (userId == null || conversationId == null) {
                return contextualMessages;
            }
            
            ConversationService.Conversation conversation = conversationService.getConversation(userId, conversationId);
            if (conversation == null) {
                return contextualMessages;
            }
            
            // Convert conversation messages to contextual messages
            List<ConversationService.ConversationMessage> messages = conversation.getMessages();
            int startIndex = Math.max(0, messages.size() - MAX_CONTEXT_HISTORY);
            
            for (int i = startIndex; i < messages.size(); i++) {
                ConversationService.ConversationMessage msg = messages.get(i);
                contextualMessages.add(new ContextualMessage(
                    msg.getRole(),
                    msg.getContent(),
                    msg.getTimestamp(),
                    extractEntitiesFromMessage(msg.getContent())
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error loading conversation history for user {} conversation {}", 
                    userId, conversationId, e);
        }
        
        return contextualMessages;
    }
    
    /**
     * Find matching entity in conversation history
     */
    private ContextualEntity findMatchingEntity(String referenceObject, List<ContextualMessage> history) {
        String lowerRef = referenceObject.toLowerCase();
        
        // Search through recent messages for matching entities
        for (int i = history.size() - 1; i >= 0; i--) {
            ContextualMessage message = history.get(i);
            
            for (ContextualEntity entity : message.getEntities()) {
                if (entity.getValue().toLowerCase().contains(lowerRef) || 
                    lowerRef.contains(entity.getValue().toLowerCase())) {
                    return entity;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract direct references (countries, products, etc.)
     */
    private List<ContextualEntity> extractDirectReferences(String query, List<ContextualMessage> history) {
        List<ContextualEntity> references = new ArrayList<>();
        
        // Simple keyword matching for common trade entities
        String[] countryKeywords = {"germany", "japan", "usa", "us", "china", "canada", "mexico"};
        String[] productKeywords = {"vehicles", "cars", "electronics", "textiles", "machinery"};
        
        String lowerQuery = query.toLowerCase();
        
        for (String country : countryKeywords) {
            if (lowerQuery.contains(country)) {
                references.add(new ContextualEntity("country", country, 0.8));
            }
        }
        
        for (String product : productKeywords) {
            if (lowerQuery.contains(product)) {
                references.add(new ContextualEntity("product", product, 0.8));
            }
        }
        
        return references;
    }
    
    /**
     * Extract entities from message content
     */
    private List<ContextualEntity> extractEntitiesFromMessage(String content) {
        List<ContextualEntity> entities = new ArrayList<>();
        
        // Simple entity extraction - would be enhanced with NLP in production
        String lowerContent = content.toLowerCase();
        
        if (lowerContent.contains("tariff") || lowerContent.contains("duty")) {
            entities.add(new ContextualEntity("concept", "tariff", 0.9));
        }
        
        if (lowerContent.contains("agreement") || lowerContent.contains("trade deal")) {
            entities.add(new ContextualEntity("concept", "trade_agreement", 0.9));
        }
        
        return entities;
    }
    
    /**
     * Extract entities from AI response
     */
    private List<ContextualEntity> extractEntitiesFromResponse(String response) {
        // This would be enhanced with proper NLP entity extraction
        return extractEntitiesFromMessage(response);
    }
    
    /**
     * Update user preferences based on query patterns
     */
    private void updateUserPreferences(UserContext userContext, String query) {
        UserPreferences preferences = userContext.getPreferences();
        
        // Track query patterns
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("compare") || lowerQuery.contains("vs") || lowerQuery.contains("versus")) {
            preferences.incrementPreference("comparison_queries");
        }
        
        if (lowerQuery.contains("cost") || lowerQuery.contains("price") || lowerQuery.contains("expensive")) {
            preferences.incrementPreference("cost_focused");
        }
        
        if (lowerQuery.contains("compliance") || lowerQuery.contains("regulation") || lowerQuery.contains("legal")) {
            preferences.incrementPreference("compliance_focused");
        }
    }
    
    /**
     * Update conversation summary for quick reference
     */
    private void updateConversationSummary(String conversationId, String query, String response) {
        // This would maintain a summary of key topics discussed
        // For now, just log the update
        logger.debug("Updated conversation summary for {}", conversationId);
    }
}