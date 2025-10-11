package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when tool execution fails
 */
public class ToolExecutionException extends ChatbotException {
    
    private String toolName;
    
    public ToolExecutionException(String toolName, String message) {
        super(message, generateSuggestionForTool(toolName));
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(message, generateSuggestionForTool(toolName), cause);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion) {
        super(message, suggestion);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, String suggestion, Throwable cause) {
        super(message, suggestion, cause);
        this.toolName = toolName;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    private static String generateSuggestionForTool(String toolName) {
        if (toolName == null) {
            return "Please try rephrasing your question or use the manual search features.";
        }
        
        switch (toolName.toLowerCase()) {
            case "tarifflookuptool":
            case "gettariffrate":
                return "Try using the **Calculator** page to manually look up tariff rates, or:\n" +
                       "• Check if you've specified both origin and destination countries\n" +
                       "• Verify the product description or HS code is correct\n" +
                       "• Try a more general product description";
                       
            case "hscodefindetool":
            case "findhscodeforproduct":
                return "Try using the **Database** page to search for HS codes, or:\n" +
                       "• Use more specific product descriptions\n" +
                       "• Include material composition (e.g., 'cotton shirt' vs 'shirt')\n" +
                       "• Try different synonyms for your product";
                       
            case "agreementtool":
            case "getagreementsbycountry":
                return "Try using the **Database** page to explore trade agreements, or:\n" +
                       "• Check if the country name is spelled correctly\n" +
                       "• Try using the full country name instead of abbreviations\n" +
                       "• Browse countries alphabetically if unsure of the exact name";
                       
            default:
                return "Please try rephrasing your question or use the manual search features in the Calculator or Database pages.";
        }
    }
}