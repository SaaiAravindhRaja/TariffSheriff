package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.ai.context.QueryContext;
import com.tariffsheriff.backend.ai.context.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Enhanced service for providing intelligent fallback responses and progressive degradation
 */
@Service
public class FallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);
    
    private final ChatCacheService cacheService;
    private final ConversationService conversationService;
    
    // Enhanced fallback strategies
    private final Map<String, FallbackStrategy> fallbackStrategies = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailureTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> failureCount = new ConcurrentHashMap<>();
    
    @Autowired
    public FallbackService(ChatCacheService cacheService, ConversationService conversationService) {
        this.cacheService = cacheService;
        this.conversationService = conversationService;
        initializeFallbackStrategies();
    }
    
    // Patterns for different types of queries
    private static final Pattern TARIFF_PATTERN = Pattern.compile(
            "(?i).*\\b(tariff|duty|rate|import|export|cost|price)\\b.*", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HS_CODE_PATTERN = Pattern.compile(
            "(?i).*\\b(hs\\s*code|harmonized|classification|product|item)\\b.*", 
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern AGREEMENT_PATTERN = Pattern.compile(
            "(?i).*\\b(agreement|treaty|trade\\s*deal|fta|partnership)\\b.*", 
            Pattern.CASE_INSENSITIVE
    );
    
    // Common country names and codes
    private static final Map<String, String> COUNTRY_SUGGESTIONS = Map.of(
            "usa", "United States",
            "us", "United States", 
            "america", "United States",
            "uk", "United Kingdom",
            "canada", "Canada",
            "mexico", "Mexico",
            "china", "China",
            "japan", "Japan",
            "germany", "Germany",
            "france", "France"
    );
    
    /**
     * Generate intelligent fallback response with multiple strategies
     */
    public ChatQueryResponse generateFallbackResponse(String query, String conversationId, long startTime) {
        return generateIntelligentFallbackResponse(query, conversationId, null, null, startTime);
    }
    
    /**
     * Generate intelligent fallback response with context and user information
     */
    public ChatQueryResponse generateIntelligentFallbackResponse(String query, String conversationId, 
                                                               String userId, QueryContext context, long startTime) {
        logger.info("Generating intelligent fallback response for query: {}", query);
        
        try {
            // Try multiple fallback strategies in order of preference
            FallbackResult result = tryFallbackStrategies(query, conversationId, userId, context);
            
            ChatQueryResponse response = new ChatQueryResponse();
            response.setResponse(result.getMessage());
            response.setConversationId(conversationId);
            response.setSuccess(result.isSuccess());
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setToolsUsed(result.getToolsUsed());
            
            // Record fallback usage for improvement
            recordFallbackUsage(result.getStrategy(), query, result.isSuccess());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in intelligent fallback", e);
            return generateBasicFallbackResponse(query, conversationId, startTime);
        }
    }
    
    /**
     * Try multiple fallback strategies
     */
    private FallbackResult tryFallbackStrategies(String query, String conversationId, 
                                               String userId, QueryContext context) {
        // Strategy 1: Try cached similar queries
        FallbackResult cacheResult = tryCachedFallback(query);
        if (cacheResult.isSuccess()) {
            return cacheResult;
        }
        
        // Strategy 2: Try conversation history fallback
        if (userId != null) {
            FallbackResult historyResult = tryConversationHistoryFallback(query, userId);
            if (historyResult.isSuccess()) {
                return historyResult;
            }
        }
        
        // Strategy 3: Try contextual fallback
        if (context != null) {
            FallbackResult contextResult = tryContextualFallback(query, context);
            if (contextResult.isSuccess()) {
                return contextResult;
            }
        }
        
        // Strategy 4: Try pattern-based intelligent fallback
        FallbackResult patternResult = tryPatternBasedFallback(query);
        if (patternResult.isSuccess()) {
            return patternResult;
        }
        
        // Strategy 5: Default to basic fallback
        return new FallbackResult("BASIC", analyzeFallbackQuery(query), true, List.of("fallback"));
    }
    
    /**
     * Try cached fallback strategy
     */
    private FallbackResult tryCachedFallback(String query) {
        try {
            // Look for similar cached queries
            ChatQueryResponse cachedResponse = cacheService.getCachedResponse(query);
            if (cachedResponse != null) {
                String message = "Based on a similar previous query:\n\n" + cachedResponse.getResponse() + 
                               "\n\n*Note: This is from cached data and may not be current.*";
                return new FallbackResult("CACHED", message, true, List.of("cache", "fallback"));
            }
            
            // Try fuzzy matching for similar queries
            String similarQuery = findSimilarCachedQuery(query);
            if (similarQuery != null) {
                ChatQueryResponse similarResponse = cacheService.getCachedResponse(similarQuery);
                if (similarResponse != null) {
                    String message = "I found information for a similar query:\n\n" + 
                                   similarResponse.getResponse() + 
                                   "\n\n*Note: This may not exactly match your question.*";
                    return new FallbackResult("SIMILAR_CACHED", message, true, List.of("cache", "fallback"));
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error in cached fallback strategy", e);
        }
        
        return new FallbackResult("CACHED", null, false, Collections.emptyList());
    }
    
    /**
     * Try conversation history fallback
     */
    private FallbackResult tryConversationHistoryFallback(String query, String userId) {
        try {
            // Get user's recent conversations
            List<ConversationService.ConversationSummary> conversations = 
                    conversationService.getUserConversations(userId);
            
            if (!conversations.isEmpty()) {
                // Look for similar topics in recent conversations
                String relatedInfo = findRelatedConversationInfo(query, conversations, userId);
                if (relatedInfo != null) {
                    String message = "Based on your recent conversations:\n\n" + relatedInfo + 
                                   "\n\n*This is from your conversation history and may help with your current question.*";
                    return new FallbackResult("HISTORY", message, true, List.of("history", "fallback"));
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error in conversation history fallback", e);
        }
        
        return new FallbackResult("HISTORY", null, false, Collections.emptyList());
    }
    
    /**
     * Try contextual fallback using query context
     */
    private FallbackResult tryContextualFallback(String query, QueryContext context) {
        try {
            if (!context.getReferencedEntities().isEmpty()) {
                StringBuilder message = new StringBuilder();
                message.append("I understand you're asking about:\n");
                
                context.getReferencedEntities().forEach(entity -> {
                    message.append("• ").append(entity.getType()).append(": ").append(entity.getValue()).append("\n");
                });
                
                message.append("\nWhile I can't process your full request right now, you can:\n");
                message.append("• Use the Calculator for tariff calculations\n");
                message.append("• Browse the Database for detailed information\n");
                message.append("• Try rephrasing your question\n");
                
                return new FallbackResult("CONTEXTUAL", message.toString(), true, List.of("context", "fallback"));
            }
            
        } catch (Exception e) {
            logger.warn("Error in contextual fallback", e);
        }
        
        return new FallbackResult("CONTEXTUAL", null, false, Collections.emptyList());
    }
    
    /**
     * Try pattern-based intelligent fallback
     */
    private FallbackResult tryPatternBasedFallback(String query) {
        try {
            String queryType = classifyQueryType(query);
            FallbackStrategy strategy = fallbackStrategies.get(queryType);
            
            if (strategy != null) {
                String message = strategy.generateResponse(query);
                return new FallbackResult("PATTERN_" + queryType, message, true, List.of("pattern", "fallback"));
            }
            
        } catch (Exception e) {
            logger.warn("Error in pattern-based fallback", e);
        }
        
        return new FallbackResult("PATTERN", null, false, Collections.emptyList());
    }
    
    /**
     * Generate basic fallback response (original method)
     */
    private ChatQueryResponse generateBasicFallbackResponse(String query, String conversationId, long startTime) {
        String fallbackMessage = analyzeFallbackQuery(query);
        
        ChatQueryResponse response = new ChatQueryResponse();
        response.setResponse(fallbackMessage);
        response.setConversationId(conversationId);
        response.setSuccess(true);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setToolsUsed(List.of("fallback"));
        
        return response;
    }
    
    /**
     * Handle progressive degradation when some tools are unavailable
     */
    public ChatQueryResponse handleProgressiveDegradation(ChatQueryRequest request, 
                                                         List<String> unavailableTools, 
                                                         Exception originalError) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Handling progressive degradation for query with {} unavailable tools", 
                    unavailableTools.size());
            
            StringBuilder message = new StringBuilder();
            message.append("I'm experiencing some technical issues, but I can still help you:\n\n");
            
            // Identify what's still available
            List<String> availableAlternatives = identifyAvailableAlternatives(unavailableTools);
            
            if (!availableAlternatives.isEmpty()) {
                message.append("**Available options:**\n");
                for (String alternative : availableAlternatives) {
                    message.append("• ").append(alternative).append("\n");
                }
                message.append("\n");
            }
            
            // Provide specific guidance based on query type
            String queryType = classifyQueryType(request.getQuery());
            message.append(getProgressiveDegradationGuidance(queryType, unavailableTools));
            
            // Add recovery information
            message.append("\n\n**System Status:**\n");
            message.append("Some services are temporarily unavailable. ");
            message.append("Please try again in a few minutes for full functionality.");
            
            ChatQueryResponse response = new ChatQueryResponse();
            response.setResponse(message.toString());
            response.setConversationId(request.getConversationId());
            response.setSuccess(true);
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            response.setToolsUsed(List.of("progressive_degradation"));
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in progressive degradation handling", e);
            return generateBasicFallbackResponse(request.getQuery(), request.getConversationId(), startTime);
        }
    }
    
    /**
     * Create user-friendly error explanations with actionable suggestions
     */
    public String createUserFriendlyErrorExplanation(Exception error, String query) {
        StringBuilder explanation = new StringBuilder();
        
        // Classify error type
        String errorType = classifyErrorType(error);
        
        switch (errorType) {
            case "LLM_SERVICE_ERROR":
                explanation.append("I'm having trouble with my language processing right now. ");
                explanation.append("This usually resolves quickly.");
                break;
                
            case "TOOL_EXECUTION_ERROR":
                explanation.append("I encountered an issue accessing the trade data you requested. ");
                explanation.append("The data sources might be temporarily unavailable.");
                break;
                
            case "RATE_LIMIT_ERROR":
                explanation.append("I'm receiving a high volume of requests right now. ");
                explanation.append("Please wait a moment before trying again.");
                break;
                
            case "VALIDATION_ERROR":
                explanation.append("I had trouble understanding your question. ");
                explanation.append("Could you please rephrase it or provide more details?");
                break;
                
            default:
                explanation.append("I encountered an unexpected issue while processing your request.");
        }
        
        // Add actionable suggestions
        explanation.append("\n\n**What you can try:**\n");
        explanation.append(getActionableSuggestions(errorType, query));
        
        return explanation.toString();
    }
    
    /**
     * Analyze query and provide appropriate fallback response
     */
    private String analyzeFallbackQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return getGeneralHelpMessage();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Check for tariff-related queries
        if (TARIFF_PATTERN.matcher(lowerQuery).matches()) {
            return getTariffFallbackMessage(query);
        }
        
        // Check for HS code queries
        if (HS_CODE_PATTERN.matcher(lowerQuery).matches()) {
            return getHsCodeFallbackMessage(query);
        }
        
        // Check for agreement queries
        if (AGREEMENT_PATTERN.matcher(lowerQuery).matches()) {
            return getAgreementFallbackMessage(query);
        }
        
        // Check if query contains country names
        String detectedCountry = detectCountryInQuery(lowerQuery);
        if (detectedCountry != null) {
            return getCountrySpecificFallbackMessage(detectedCountry);
        }
        
        // Default fallback
        return getGeneralFallbackMessage();
    }
    
    /**
     * Generate tariff-specific fallback message
     */
    private String getTariffFallbackMessage(String query) {
        StringBuilder message = new StringBuilder();
        message.append("I'm currently unable to process your tariff inquiry, but I can help guide you to the right information.\n\n");
        
        message.append("**To find tariff rates manually:**\n");
        message.append("1. Go to the Calculator page\n");
        message.append("2. Select your origin and destination countries\n");
        message.append("3. Enter the HS code or product description\n");
        message.append("4. Click 'Calculate' to see both MFN and preferential rates\n\n");
        
        message.append("**Common tariff questions I can help with when available:**\n");
        message.append("• \"What's the tariff for importing [product] from [country] to [country]?\"\n");
        message.append("• \"Show me duty rates for HS code [code] between [countries]\"\n");
        message.append("• \"Compare tariff rates for [product] from different countries\"\n\n");
        
        // Add official resources
        List<ResourceSuggestion> resources = suggestOfficialResources(query, "TARIFF");
        message.append(formatResourceSuggestions(resources));
        
        message.append("\n\nPlease try your question again in a few moments, or use the Calculator for immediate results.");
        
        return message.toString();
    }
    
    /**
     * Generate HS code specific fallback message
     */
    private String getHsCodeFallbackMessage(String query) {
        StringBuilder message = new StringBuilder();
        message.append("I'm currently unable to help with HS code classification, but here's how you can find the right code:\n\n");
        
        message.append("**To find HS codes manually:**\n");
        message.append("1. Go to the Database page\n");
        message.append("2. Use the product search to find similar items\n");
        message.append("3. Browse by category if you know the general product type\n");
        message.append("4. Check the detailed descriptions to find the best match\n\n");
        
        message.append("**Tips for better HS code searches:**\n");
        message.append("• Be specific about materials (e.g., 'cotton shirt' vs 'shirt')\n");
        message.append("• Include key characteristics (size, use, composition)\n");
        message.append("• Try different synonyms if the first search doesn't work\n\n");
        
        message.append("**Common HS code questions I can help with when available:**\n");
        message.append("• \"What's the HS code for [specific product description]?\"\n");
        message.append("• \"Find HS codes for products containing [material]\"\n");
        message.append("• \"Show me all codes in chapter [number]\"\n\n");
        
        // Add official resources
        List<ResourceSuggestion> resources = suggestOfficialResources(query, "HS_CODE");
        message.append(formatResourceSuggestions(resources));
        
        message.append("\n\nPlease try again in a moment, or use the Database search for immediate results.");
        
        return message.toString();
    }
    
    /**
     * Generate agreement-specific fallback message
     */
    private String getAgreementFallbackMessage(String query) {
        StringBuilder message = new StringBuilder();
        message.append("I'm currently unable to process your trade agreement inquiry, but here's how to find this information:\n\n");
        
        message.append("**To explore trade agreements manually:**\n");
        message.append("1. Go to the Database page\n");
        message.append("2. Select a country from the country list\n");
        message.append("3. View the country's trade agreements and partners\n");
        message.append("4. Click on specific agreements for more details\n\n");
        
        message.append("**Common agreement questions I can help with when available:**\n");
        message.append("• \"What trade agreements does [country] have?\"\n");
        message.append("• \"Is there a trade agreement between [country1] and [country2]?\"\n");
        message.append("• \"Show me all FTA partners for [country]\"\n\n");
        
        // Add official resources
        List<ResourceSuggestion> resources = suggestOfficialResources(query, "AGREEMENT");
        message.append(formatResourceSuggestions(resources));
        
        message.append("\n\nPlease try your question again in a few moments, or browse the Database for immediate access to agreement information.");
        
        return message.toString();
    }
    
    /**
     * Generate country-specific fallback message
     */
    private String getCountrySpecificFallbackMessage(String country) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("I'm currently unable to process your inquiry about %s, but here's how to find this information:\n\n", country));
        
        message.append("**To explore country-specific trade data:**\n");
        message.append("1. Go to the Database page\n");
        message.append(String.format("2. Search for '%s' in the country list\n", country));
        message.append("3. View trade agreements, tariff schedules, and economic indicators\n");
        message.append("4. Use the Calculator for specific tariff calculations\n\n");
        
        // Add official resources with country context
        List<ResourceSuggestion> resources = suggestOfficialResources("country " + country, "COUNTRY");
        message.append(formatResourceSuggestions(resources));
        
        message.append("\n\nPlease try your question again in a few moments, or explore the country data directly through the Database.");
        
        return message.toString();
    }
    
    /**
     * Generate general fallback message
     */
    private String getGeneralFallbackMessage() {
        StringBuilder message = new StringBuilder();
        message.append("I'm currently experiencing technical difficulties and can't process your request right now.\n\n");
        
        message.append("**While I'm unavailable, you can:**\n");
        message.append("• Use the **Calculator** to find tariff rates between countries\n");
        message.append("• Browse the **Database** to explore trade agreements and country data\n");
        message.append("• Visit the **Analytics** page for trade insights and trends\n\n");
        
        message.append("**When I'm back online, I can help you with:**\n");
        message.append("• Finding tariff rates for specific products and trade routes\n");
        message.append("• Identifying HS codes from product descriptions\n");
        message.append("• Explaining trade agreements between countries\n");
        message.append("• Comparing trade costs and opportunities\n\n");
        
        message.append("Please try your question again in a few moments. Thank you for your patience!");
        
        return message.toString();
    }
    
    /**
     * Generate general help message
     */
    private String getGeneralHelpMessage() {
        StringBuilder message = new StringBuilder();
        message.append("Hello! I'm your AI Trade Assistant. I can help you with:\n\n");
        
        message.append("**🔍 Tariff Lookups**\n");
        message.append("• \"What's the tariff for importing coffee from Brazil to the US?\"\n");
        message.append("• \"Show me duty rates for electronics from China\"\n\n");
        
        message.append("**📋 HS Code Classification**\n");
        message.append("• \"What's the HS code for leather handbags?\"\n");
        message.append("• \"Find the classification for electric vehicles\"\n\n");
        
        message.append("**🤝 Trade Agreements**\n");
        message.append("• \"What trade agreements does Canada have?\"\n");
        message.append("• \"Is there an FTA between Japan and Australia?\"\n\n");
        
        message.append("**💡 Tips for better results:**\n");
        message.append("• Be specific about products and countries\n");
        message.append("• Ask one question at a time\n");
        message.append("• Include relevant details like materials or intended use\n\n");
        
        message.append("What would you like to know about international trade?");
        
        return message.toString();
    }
    
    /**
     * Detect country names in query
     */
    private String detectCountryInQuery(String query) {
        for (Map.Entry<String, String> entry : COUNTRY_SUGGESTIONS.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Check if a query seems to be asking for help or general information
     */
    public boolean isHelpQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        List<String> helpKeywords = Arrays.asList(
                "help", "hello", "hi", "what can you do", "how do you work", 
                "what are you", "capabilities", "features", "guide", "tutorial"
        );
        
        return helpKeywords.stream().anyMatch(lowerQuery::contains);
    }
    
    /**
     * Suggest official resources based on query type and context
     */
    public List<ResourceSuggestion> suggestOfficialResources(String query, String queryType) {
        List<ResourceSuggestion> suggestions = new ArrayList<>();
        
        if (queryType == null) {
            queryType = classifyQueryType(query);
        }
        
        switch (queryType) {
            case "TARIFF":
                suggestions.add(new ResourceSuggestion(
                    "WTO Tariff Database",
                    "https://www.wto.org/english/tratop_e/tariffs_e/tariff_data_e.htm",
                    "Official World Trade Organization tariff data and analysis",
                    "Comprehensive global tariff information"
                ));
                suggestions.add(new ResourceSuggestion(
                    "USITC DataWeb",
                    "https://dataweb.usitc.gov/",
                    "U.S. International Trade Commission trade and tariff data",
                    "Detailed U.S. import/export statistics"
                ));
                suggestions.add(new ResourceSuggestion(
                    "Trade.gov",
                    "https://www.trade.gov/",
                    "U.S. Department of Commerce trade resources",
                    "Market research and trade regulations"
                ));
                break;
                
            case "HS_CODE":
                suggestions.add(new ResourceSuggestion(
                    "WCO HS Nomenclature",
                    "http://www.wcoomd.org/en/topics/nomenclature/overview.aspx",
                    "World Customs Organization Harmonized System classification",
                    "Official HS code classification system"
                ));
                suggestions.add(new ResourceSuggestion(
                    "USITC HTS Search",
                    "https://hts.usitc.gov/",
                    "U.S. Harmonized Tariff Schedule search tool",
                    "Search and browse U.S. tariff classifications"
                ));
                suggestions.add(new ResourceSuggestion(
                    "Census Bureau Schedule B",
                    "https://www.census.gov/foreign-trade/schedules/b/",
                    "U.S. export classification codes",
                    "Classification for U.S. exports"
                ));
                break;
                
            case "AGREEMENT":
                suggestions.add(new ResourceSuggestion(
                    "WTO Regional Trade Agreements",
                    "https://www.wto.org/english/tratop_e/region_e/region_e.htm",
                    "Database of regional trade agreements notified to the WTO",
                    "Comprehensive FTA and RTA information"
                ));
                suggestions.add(new ResourceSuggestion(
                    "USTR Trade Agreements",
                    "https://ustr.gov/trade-agreements",
                    "U.S. Trade Representative agreements and negotiations",
                    "U.S. trade agreement details and texts"
                ));
                suggestions.add(new ResourceSuggestion(
                    "Trade.gov FTA Portal",
                    "https://www.trade.gov/fta",
                    "U.S. Free Trade Agreement resources and guidance",
                    "Practical FTA utilization information"
                ));
                break;
                
            case "COUNTRY":
                String country = detectCountryInQuery(query.toLowerCase());
                suggestions.add(new ResourceSuggestion(
                    "WTO Country Profiles",
                    "https://www.wto.org/english/thewto_e/countries_e/countries_e.htm",
                    "Official WTO member country trade profiles",
                    "Trade policies and statistics by country"
                ));
                suggestions.add(new ResourceSuggestion(
                    "Trade.gov Country Commercial Guides",
                    "https://www.trade.gov/country-commercial-guides",
                    "Market conditions and opportunities by country",
                    "Detailed country market analysis"
                ));
                if (country != null) {
                    suggestions.add(new ResourceSuggestion(
                        country + " Customs Authority",
                        "https://www.google.com/search?q=" + country.replace(" ", "+") + "+customs+authority",
                        "Official customs authority for " + country,
                        "Country-specific customs regulations"
                    ));
                }
                break;
                
            default:
                // General trade resources
                suggestions.add(new ResourceSuggestion(
                    "WTO Resources",
                    "https://www.wto.org/",
                    "World Trade Organization official website",
                    "Global trade rules and information"
                ));
                suggestions.add(new ResourceSuggestion(
                    "Trade.gov",
                    "https://www.trade.gov/",
                    "U.S. Department of Commerce trade portal",
                    "Comprehensive U.S. trade resources"
                ));
                suggestions.add(new ResourceSuggestion(
                    "International Trade Centre",
                    "https://www.intracen.org/",
                    "Trade statistics and market analysis",
                    "Trade data and business tools"
                ));
        }
        
        return suggestions;
    }
    
    /**
     * Format resource suggestions as a readable string
     */
    public String formatResourceSuggestions(List<ResourceSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("\n\n**📚 Official Resources:**\n");
        
        for (ResourceSuggestion suggestion : suggestions) {
            formatted.append("\n**").append(suggestion.getName()).append("**\n");
            formatted.append("🔗 ").append(suggestion.getUrl()).append("\n");
            formatted.append("ℹ️ ").append(suggestion.getDescription()).append("\n");
        }
        
        return formatted.toString();
    }
    
    /**
     * Enhanced data-not-found response with helpful next steps
     */
    public String generateDataNotFoundResponse(String query, String dataType, String specificReason) {
        StringBuilder response = new StringBuilder();
        
        // Acknowledge what they were looking for
        response.append("I couldn't find ");
        if (dataType != null) {
            response.append(dataType).append(" ");
        }
        response.append("information for your query");
        
        if (specificReason != null && !specificReason.isEmpty()) {
            response.append(" (").append(specificReason).append(")");
        }
        response.append(".\n\n");
        
        // Provide context about data availability
        response.append("**What this means:**\n");
        response.append("• The specific data you requested may not be in our database\n");
        response.append("• The product, country, or agreement might use different terminology\n");
        response.append("• The information might be too recent or specialized\n\n");
        
        // Suggest alternative approaches
        response.append("**What you can try:**\n");
        
        String queryType = classifyQueryType(query);
        switch (queryType) {
            case "TARIFF":
                response.append("• Try searching with an HS code instead of product description\n");
                response.append("• Check if country names are spelled correctly (use full names)\n");
                response.append("• Look for similar products in the Database\n");
                response.append("• Use the Calculator with known HS codes\n");
                break;
                
            case "HS_CODE":
                response.append("• Try a more general product description\n");
                response.append("• Include material composition (e.g., 'cotton' or 'plastic')\n");
                response.append("• Browse by product category in the Database\n");
                response.append("• Try different synonyms or related terms\n");
                break;
                
            case "AGREEMENT":
                response.append("• Verify country names are spelled correctly\n");
                response.append("• Try searching for both countries individually\n");
                response.append("• Check if the agreement uses a different name or acronym\n");
                response.append("• Browse all agreements in the Database\n");
                break;
                
            default:
                response.append("• Rephrase your question with more specific details\n");
                response.append("• Break complex questions into smaller parts\n");
                response.append("• Use the Database to explore available information\n");
                response.append("• Try the Calculator for manual lookups\n");
        }
        
        // Add official resource suggestions
        List<ResourceSuggestion> resources = suggestOfficialResources(query, queryType);
        if (!resources.isEmpty()) {
            response.append(formatResourceSuggestions(resources));
        }
        
        response.append("\n\n💡 **Tip:** If you need current or specialized information, ");
        response.append("the official resources above are authoritative sources.");
        
        return response.toString();
    }
    
    // ========== MISSING METHODS IMPLEMENTATION ==========
    
    /**
     * Initialize fallback strategies for different query types
     */
    private void initializeFallbackStrategies() {
        fallbackStrategies.put("TARIFF", new TariffFallbackStrategy());
        fallbackStrategies.put("HS_CODE", new HsCodeFallbackStrategy());
        fallbackStrategies.put("AGREEMENT", new AgreementFallbackStrategy());
        fallbackStrategies.put("COUNTRY", new CountryFallbackStrategy());
        fallbackStrategies.put("GENERAL", new GeneralFallbackStrategy());
    }
    
    /**
     * Classify query type for appropriate fallback strategy
     */
    private String classifyQueryType(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (TARIFF_PATTERN.matcher(lowerQuery).matches()) {
            return "TARIFF";
        }
        if (HS_CODE_PATTERN.matcher(lowerQuery).matches()) {
            return "HS_CODE";
        }
        if (AGREEMENT_PATTERN.matcher(lowerQuery).matches()) {
            return "AGREEMENT";
        }
        if (detectCountryInQuery(lowerQuery) != null) {
            return "COUNTRY";
        }
        
        return "GENERAL";
    }
    
    /**
     * Find similar cached query using fuzzy matching
     */
    private String findSimilarCachedQuery(String query) {
        try {
            // Simple similarity check - in production, use more sophisticated algorithms
            Set<String> cachedQueries = cacheService.getCachedQueryKeys();
            String normalizedQuery = query.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
            
            for (String cachedQuery : cachedQueries) {
                String normalizedCached = cachedQuery.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
                if (calculateSimilarity(normalizedQuery, normalizedCached) > 0.7) {
                    return cachedQuery;
                }
            }
        } catch (Exception e) {
            logger.warn("Error finding similar cached query", e);
        }
        return null;
    }
    
    /**
     * Calculate simple similarity between two strings
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Find related information from conversation history
     */
    private String findRelatedConversationInfo(String query, 
                                             List<ConversationService.ConversationSummary> conversations, 
                                             String userId) {
        try {
            String queryType = classifyQueryType(query);
            
            for (ConversationService.ConversationSummary conversation : conversations) {
                if (conversation.getLastMessage() != null && 
                    classifyQueryType(conversation.getLastMessage()).equals(queryType)) {
                    
                    // Get the actual conversation content
                    String conversationContent = conversationService.getConversationContent(
                            conversation.getConversationId(), userId);
                    
                    if (conversationContent != null && conversationContent.length() > 100) {
                        return conversationContent.substring(0, Math.min(500, conversationContent.length())) + "...";
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error finding related conversation info", e);
        }
        return null;
    }
    
    /**
     * Identify available alternatives when some tools are unavailable
     */
    private List<String> identifyAvailableAlternatives(List<String> unavailableTools) {
        List<String> alternatives = new ArrayList<>();
        
        if (!unavailableTools.contains("calculator")) {
            alternatives.add("Use the Calculator for manual tariff lookups");
        }
        if (!unavailableTools.contains("database")) {
            alternatives.add("Browse the Database for country and product information");
        }
        if (!unavailableTools.contains("analytics")) {
            alternatives.add("Check Analytics for trade trends and insights");
        }
        if (!unavailableTools.contains("cache")) {
            alternatives.add("Access cached information from previous queries");
        }
        
        return alternatives;
    }
    
    /**
     * Get progressive degradation guidance based on query type
     */
    private String getProgressiveDegradationGuidance(String queryType, List<String> unavailableTools) {
        StringBuilder guidance = new StringBuilder();
        
        switch (queryType) {
            case "TARIFF":
                guidance.append("**For tariff information:**\n");
                if (!unavailableTools.contains("calculator")) {
                    guidance.append("• Use the Calculator page for manual tariff lookups\n");
                }
                guidance.append("• Try rephrasing your question with specific country names\n");
                guidance.append("• Include HS codes if you know them\n");
                break;
                
            case "HS_CODE":
                guidance.append("**For HS code classification:**\n");
                if (!unavailableTools.contains("database")) {
                    guidance.append("• Browse the Database by product category\n");
                }
                guidance.append("• Try searching with different product descriptions\n");
                guidance.append("• Include material composition and intended use\n");
                break;
                
            case "AGREEMENT":
                guidance.append("**For trade agreement information:**\n");
                if (!unavailableTools.contains("database")) {
                    guidance.append("• Check the Database country profiles\n");
                }
                guidance.append("• Look up specific country pairs\n");
                guidance.append("• Try searching for agreement acronyms (FTA, CPTPP, etc.)\n");
                break;
                
            default:
                guidance.append("**General suggestions:**\n");
                guidance.append("• Try breaking your question into smaller parts\n");
                guidance.append("• Use specific country and product names\n");
                guidance.append("• Check back in a few minutes for full functionality\n");
        }
        
        return guidance.toString();
    }
    
    /**
     * Classify error type for appropriate handling
     */
    private String classifyErrorType(Exception error) {
        String errorClass = error.getClass().getSimpleName();
        String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        
        if (errorClass.contains("LlmService") || errorMessage.contains("llm") || errorMessage.contains("language model")) {
            return "LLM_SERVICE_ERROR";
        }
        if (errorClass.contains("ToolExecution") || errorMessage.contains("tool") || errorMessage.contains("execution")) {
            return "TOOL_EXECUTION_ERROR";
        }
        if (errorClass.contains("RateLimit") || errorMessage.contains("rate limit") || errorMessage.contains("too many requests")) {
            return "RATE_LIMIT_ERROR";
        }
        if (errorClass.contains("Validation") || errorMessage.contains("validation") || errorMessage.contains("invalid")) {
            return "VALIDATION_ERROR";
        }
        
        return "UNKNOWN_ERROR";
    }
    
    /**
     * Get actionable suggestions based on error type
     */
    private String getActionableSuggestions(String errorType, String query) {
        StringBuilder suggestions = new StringBuilder();
        
        switch (errorType) {
            case "LLM_SERVICE_ERROR":
                suggestions.append("• Wait 1-2 minutes and try again\n");
                suggestions.append("• Use the Calculator or Database for immediate results\n");
                suggestions.append("• Try a simpler version of your question\n");
                break;
                
            case "TOOL_EXECUTION_ERROR":
                suggestions.append("• Check if your query includes valid country names\n");
                suggestions.append("• Try using standard country codes (US, CA, DE, etc.)\n");
                suggestions.append("• Use the manual tools while I recover\n");
                break;
                
            case "RATE_LIMIT_ERROR":
                suggestions.append("• Wait 30 seconds before trying again\n");
                suggestions.append("• Use the Database to browse information manually\n");
                suggestions.append("• Try during off-peak hours for faster responses\n");
                break;
                
            case "VALIDATION_ERROR":
                suggestions.append("• Include specific country names in your question\n");
                suggestions.append("• Be more specific about the product or service\n");
                suggestions.append("• Try: 'What is the tariff for [product] from [country] to [country]?'\n");
                break;
                
            default:
                suggestions.append("• Try rephrasing your question\n");
                suggestions.append("• Use the Calculator or Database as alternatives\n");
                suggestions.append("• Contact support if the problem persists\n");
        }
        
        return suggestions.toString();
    }
    
    /**
     * Record fallback usage for improvement
     */
    private void recordFallbackUsage(String strategy, String query, boolean success) {
        try {
            String key = strategy + "_" + classifyQueryType(query);
            
            // Update failure tracking
            if (!success) {
                failureCount.merge(key, 1, Integer::sum);
                lastFailureTime.put(key, LocalDateTime.now());
            }
            
            logger.info("Fallback strategy {} used for query type {}, success: {}", 
                    strategy, classifyQueryType(query), success);
            
        } catch (Exception e) {
            logger.warn("Error recording fallback usage", e);
        }
    }
    
    // ========== FALLBACK STRATEGY IMPLEMENTATIONS ==========
    
    /**
     * Fallback strategy interface
     */
    private interface FallbackStrategy {
        String generateResponse(String query);
    }
    
    /**
     * Tariff-specific fallback strategy
     */
    private class TariffFallbackStrategy implements FallbackStrategy {
        @Override
        public String generateResponse(String query) {
            return getTariffFallbackMessage(query);
        }
    }
    
    /**
     * HS Code fallback strategy
     */
    private class HsCodeFallbackStrategy implements FallbackStrategy {
        @Override
        public String generateResponse(String query) {
            return getHsCodeFallbackMessage(query);
        }
    }
    
    /**
     * Agreement fallback strategy
     */
    private class AgreementFallbackStrategy implements FallbackStrategy {
        @Override
        public String generateResponse(String query) {
            return getAgreementFallbackMessage(query);
        }
    }
    
    /**
     * Country-specific fallback strategy
     */
    private class CountryFallbackStrategy implements FallbackStrategy {
        @Override
        public String generateResponse(String query) {
            String country = detectCountryInQuery(query.toLowerCase());
            return country != null ? getCountrySpecificFallbackMessage(country) : getGeneralFallbackMessage();
        }
    }
    
    /**
     * General fallback strategy
     */
    private class GeneralFallbackStrategy implements FallbackStrategy {
        @Override
        public String generateResponse(String query) {
            return getGeneralFallbackMessage();
        }
    }
    
    /**
     * Fallback result container
     */
    private static class FallbackResult {
        private final String strategy;
        private final String message;
        private final boolean success;
        private final List<String> toolsUsed;
        
        public FallbackResult(String strategy, String message, boolean success, List<String> toolsUsed) {
            this.strategy = strategy;
            this.message = message;
            this.success = success;
            this.toolsUsed = toolsUsed;
        }
        
        public String getStrategy() { return strategy; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return success; }
        public List<String> getToolsUsed() { return toolsUsed; }
    }
    
    /**
     * Resource suggestion container for official trade resources
     */
    public static class ResourceSuggestion {
        private final String name;
        private final String url;
        private final String description;
        private final String relevance;
        
        public ResourceSuggestion(String name, String url, String description, String relevance) {
            this.name = name;
            this.url = url;
            this.description = description;
            this.relevance = relevance;
        }
        
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getDescription() { return description; }
        public String getRelevance() { return relevance; }
    }
}