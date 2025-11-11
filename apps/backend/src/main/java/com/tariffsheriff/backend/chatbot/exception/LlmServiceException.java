package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when LLM service interactions fail with enhanced user guidance
 */
public class LlmServiceException extends ChatbotException {
    
    public LlmServiceException(String message) {
        super(message, makeUserFriendly(message), generateDefaultSuggestion(), null);
    }
    
    public LlmServiceException(String message, Throwable cause) {
        super(message, makeUserFriendly(message), generateDefaultSuggestion(), cause);
    }
    
    public LlmServiceException(String message, String suggestion) {
        super(message, makeUserFriendly(message), suggestion, null);
    }
    
    public LlmServiceException(String message, String suggestion, Throwable cause) {
        super(message, makeUserFriendly(message), suggestion, cause);
    }
    
    // ... rest of the class is unchanged ...
    
    /**
     * Convert technical LLM errors to user-friendly messages
     */
    private static String makeUserFriendly(String technicalMessage) {
        if (technicalMessage == null) {
            return "I'm having trouble with my AI processing right now.";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        if (lower.contains("api key") || lower.contains("authentication") || lower.contains("unauthorized")) {
            return "I'm experiencing a configuration issue. Our team has been notified.";
        }
        
        if (lower.contains("quota") || lower.contains("rate limit")) {
            return "I'm receiving a high volume of requests. Please wait a moment.";
        }
        
        if (lower.contains("timeout") || lower.contains("deadline")) {
            return "My response is taking longer than expected. Let me try again.";
        }
        
        if (lower.contains("model") || lower.contains("unavailable")) {
            return "My AI service is temporarily unavailable. Please try again shortly.";
        }
        
        return "I'm having trouble processing your request with my AI capabilities right now.";
    }
    
    /**
     * Generate helpful suggestions when LLM service is unavailable
     */
    private static String generateDefaultSuggestion() {
        return "\n\n**While I'm recovering, you can:**\n" +
               "• Use the **Calculator** page to manually look up tariff rates\n" +
               "• Browse the **Database** to explore trade agreements and country data\n" +
               "• Check the **Analytics** page for trade insights and trends\n" +
               "• Try asking your question again in 1-2 minutes\n\n" +
               "**Need immediate help?**\n" +
               "• Visit https://www.trade.gov/ for U.S. trade resources\n" +
               "• Check https://www.wto.org/ for global trade information\n" +
               "• Use https://hts.usitc.gov/ for HS code lookups";
    }
}