package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when LLM service interactions fail
 */
public class LlmServiceException extends ChatbotException {
    
    public LlmServiceException(String message) {
        super(message, generateDefaultSuggestion());
    }
    
    public LlmServiceException(String message, Throwable cause) {
        super(message, generateDefaultSuggestion(), cause);
    }
    
    public LlmServiceException(String message, String suggestion) {
        super(message, suggestion);
    }
    
    public LlmServiceException(String message, String suggestion, Throwable cause) {
        super(message, suggestion, cause);
    }
    
    private static String generateDefaultSuggestion() {
        return "While I'm unavailable, you can:\n" +
               "• Use the **Calculator** to find tariff rates\n" +
               "• Browse the **Database** for trade agreements and country data\n" +
               "• Try asking your question again in a few moments";
    }
}