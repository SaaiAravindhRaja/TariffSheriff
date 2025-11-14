package com.tariffsheriff.backend.chatbot.exception;

/**
 * Base exception for chatbot-related errors with user-friendly messaging
 */
public class ChatbotException extends RuntimeException {
    
    private String suggestion;
    private String userFriendlyMessage;
    
    public ChatbotException(String message) {
        super(message);
        this.userFriendlyMessage = makeUserFriendly(message);
    }
    
    public ChatbotException(String message, Throwable cause) {
        super(message, cause);
        this.userFriendlyMessage = makeUserFriendly(message);
    }
    
    public ChatbotException(String message, String suggestion) {
        super(message);
        this.suggestion = suggestion;
        this.userFriendlyMessage = makeUserFriendly(message);
    }
    
    public ChatbotException(String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.suggestion = suggestion;
        this.userFriendlyMessage = makeUserFriendly(message);
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }
    
    public void setUserFriendlyMessage(String userFriendlyMessage) {
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    /**
     * Convert technical error messages to user-friendly ones
     */
    private String makeUserFriendly(String technicalMessage) {
        if (technicalMessage == null) {
            return "I encountered an issue while processing your request. Please try again.";
        }
        
        String lower = technicalMessage.toLowerCase();
        
        // Connection errors
        if (lower.contains("connection") || lower.contains("timeout") || lower.contains("network")) {
            return "I'm having trouble connecting to my services right now. Please try again in a moment.";
        }
        
        // Null pointer or data errors
        if (lower.contains("null") || lower.contains("not found") || lower.contains("missing")) {
            return "I couldn't find the information you're looking for. It might not be available in our database.";
        }
        
        // Validation errors
        if (lower.contains("invalid") || lower.contains("validation") || lower.contains("format")) {
            return "I had trouble understanding your request. Could you please rephrase it with more details?";
        }
        
        // Rate limit errors
        if (lower.contains("rate") || lower.contains("limit") || lower.contains("quota")) {
            return "I'm receiving a lot of requests right now. Please wait a moment and try again.";
        }
        
        // Default user-friendly message
        return "I encountered an issue while processing your request. Please try again or rephrase your question.";
    }

    /**
     * Internal constructor for subclasses that have already processed their messages.
     */
    protected ChatbotException(String technicalMessage, String userFriendlyMessage, String suggestion, Throwable cause) {
        super(technicalMessage, cause);
        this.userFriendlyMessage = userFriendlyMessage;
        this.suggestion = suggestion;
    }
}