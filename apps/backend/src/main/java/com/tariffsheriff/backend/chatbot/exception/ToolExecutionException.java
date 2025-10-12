package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when tool execution fails with context-specific guidance
 */
public class ToolExecutionException extends ChatbotException {
    
    private String toolName;
    
    public ToolExecutionException(String toolName, String message) {
        super(makeUserFriendly(toolName, message), generateSuggestionForTool(toolName));
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(makeUserFriendly(toolName, message), generateSuggestionForTool(toolName), cause);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion) {
        super(makeUserFriendly(toolName, message), suggestion);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion, Throwable cause) {
        super(makeUserFriendly(toolName, message), suggestion, cause);
        this.toolName = toolName;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    /**
     * Convert technical tool errors to user-friendly messages
     */
    private static String makeUserFriendly(String toolName, String technicalMessage) {
        if (toolName == null) {
            return "I encountered an issue while looking up your information.";
        }
        
        String lower = toolName.toLowerCase();
        
        if (lower.contains("tariff") || lower.contains("rate")) {
            return "I couldn't retrieve the tariff information you requested.";
        }
        
        if (lower.contains("hscode") || lower.contains("classification")) {
            return "I had trouble finding the HS code classification for your product.";
        }
        
        if (lower.contains("agreement") || lower.contains("fta")) {
            return "I couldn't access the trade agreement information right now.";
        }
        
        if (lower.contains("compliance")) {
            return "I'm unable to complete the compliance analysis at this time.";
        }
        
        if (lower.contains("market") || lower.contains("intelligence")) {
            return "I couldn't retrieve the market intelligence data you requested.";
        }
        
        if (lower.contains("risk")) {
            return "I'm unable to perform the risk assessment right now.";
        }
        
        return "I encountered an issue while processing your request.";
    }
    
    /**
     * Generate tool-specific suggestions with official resources
     */
    private static String generateSuggestionForTool(String toolName) {
        if (toolName == null) {
            return "\n\n**What you can try:**\n" +
                   "• Rephrase your question with more specific details\n" +
                   "• Use the Calculator or Database pages for manual lookups\n" +
                   "• Try again in a few moments";
        }
        
        String lower = toolName.toLowerCase();
        
        if (lower.contains("tariff") || lower.contains("rate")) {
            return "\n\n**What you can try:**\n" +
                   "• Use the **Calculator** page to manually look up tariff rates\n" +
                   "• Verify both origin and destination countries are specified correctly\n" +
                   "• Check that the HS code or product description is accurate\n" +
                   "• Try a more general product description\n\n" +
                   "**Official Resources:**\n" +
                   "• WTO Tariff Database: https://www.wto.org/english/tratop_e/tariffs_e/tariff_data_e.htm\n" +
                   "• USITC DataWeb: https://dataweb.usitc.gov/\n" +
                   "• Trade.gov: https://www.trade.gov/";
        }
        
        if (lower.contains("hscode") || lower.contains("classification")) {
            return "\n\n**What you can try:**\n" +
                   "• Use the **Database** page to search for HS codes\n" +
                   "• Be more specific about materials (e.g., 'cotton shirt' vs 'shirt')\n" +
                   "• Include key characteristics (size, use, composition)\n" +
                   "• Try different synonyms or related terms\n" +
                   "• Browse by product category\n\n" +
                   "**Official Resources:**\n" +
                   "• USITC HTS Search: https://hts.usitc.gov/\n" +
                   "• WCO HS Nomenclature: http://www.wcoomd.org/en/topics/nomenclature/overview.aspx\n" +
                   "• Census Bureau Schedule B: https://www.census.gov/foreign-trade/schedules/b/";
        }
        
        if (lower.contains("agreement") || lower.contains("fta")) {
            return "\n\n**What you can try:**\n" +
                   "• Use the **Database** page to explore trade agreements\n" +
                   "• Verify country names are spelled correctly\n" +
                   "• Use full country names instead of abbreviations\n" +
                   "• Browse countries alphabetically\n" +
                   "• Search for agreement acronyms (USMCA, CPTPP, etc.)\n\n" +
                   "**Official Resources:**\n" +
                   "• WTO Regional Trade Agreements: https://www.wto.org/english/tratop_e/region_e/region_e.htm\n" +
                   "• USTR Trade Agreements: https://ustr.gov/trade-agreements\n" +
                   "• Trade.gov FTA Portal: https://www.trade.gov/fta";
        }
        
        if (lower.contains("compliance")) {
            return "\n\n**What you can try:**\n" +
                   "• Break down your compliance question into smaller parts\n" +
                   "• Specify the exact regulation or requirement you're asking about\n" +
                   "• Check the Database for country-specific requirements\n" +
                   "• Try again with more specific product details\n\n" +
                   "**Official Resources:**\n" +
                   "• Trade.gov Compliance: https://www.trade.gov/\n" +
                   "• CBP Regulations: https://www.cbp.gov/trade\n" +
                   "• WTO Trade Facilitation: https://www.wto.org/english/tratop_e/tradfa_e/tradfa_e.htm";
        }
        
        if (lower.contains("market") || lower.contains("intelligence")) {
            return "\n\n**What you can try:**\n" +
                   "• Check the **Analytics** page for market insights\n" +
                   "• Be more specific about the market or region\n" +
                   "• Try asking about specific countries or products\n" +
                   "• Use the Database to explore country profiles\n\n" +
                   "**Official Resources:**\n" +
                   "• Trade.gov Market Research: https://www.trade.gov/market-intelligence\n" +
                   "• International Trade Centre: https://www.intracen.org/\n" +
                   "• WTO Statistics: https://www.wto.org/english/res_e/statis_e/statis_e.htm";
        }
        
        if (lower.contains("risk")) {
            return "\n\n**What you can try:**\n" +
                   "• Specify the type of risk you're concerned about\n" +
                   "• Provide more details about the trade scenario\n" +
                   "• Check country-specific information in the Database\n" +
                   "• Try breaking down your risk assessment into components\n\n" +
                   "**Official Resources:**\n" +
                   "• Trade.gov Risk Assessment: https://www.trade.gov/\n" +
                   "• Export.gov Country Information: https://www.export.gov/\n" +
                   "• WTO Trade Monitoring: https://www.wto.org/english/tratop_e/tpr_e/tpr_e.htm";
        }
        
        // Default suggestion
        return "\n\n**What you can try:**\n" +
               "• Rephrase your question with more specific details\n" +
               "• Use the Calculator or Database pages for manual lookups\n" +
               "• Break complex questions into smaller parts\n" +
               "• Try again in a few moments\n\n" +
               "**General Resources:**\n" +
               "• Trade.gov: https://www.trade.gov/\n" +
               "• WTO Resources: https://www.wto.org/\n" +
               "• International Trade Centre: https://www.intracen.org/";
    }
}