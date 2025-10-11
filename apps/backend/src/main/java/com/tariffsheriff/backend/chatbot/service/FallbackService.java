package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for providing fallback responses when LLM is unavailable
 */
@Service
public class FallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);
    
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
     * Generate a fallback response when LLM is unavailable
     */
    public ChatQueryResponse generateFallbackResponse(String query, String conversationId, long startTime) {
        logger.info("Generating fallback response for query: {}", query);
        
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
        
        message.append("Please try your question again in a few moments, or use the Calculator for immediate results.");
        
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
        
        message.append("Please try again in a moment, or use the Database search for immediate results.");
        
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
        
        message.append("Please try your question again in a few moments, or browse the Database for immediate access to agreement information.");
        
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
        
        message.append("Please try your question again in a few moments, or explore the country data directly through the Database.");
        
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
}