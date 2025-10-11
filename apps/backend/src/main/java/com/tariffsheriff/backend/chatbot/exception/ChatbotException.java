package com.tariffsheriff.backend.chatbot.exception;

/**
 * Base exception for chatbot-related errors
 */
public class ChatbotException extends RuntimeException {
    
    private String suggestion;
    
    public ChatbotException(String message) {
        super(message);
    }
    
    public ChatbotException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ChatbotException(String message, String suggestion) {
        super(message);
        this.suggestion = suggestion;
    }
    
    public ChatbotException(String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.suggestion = suggestion;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}