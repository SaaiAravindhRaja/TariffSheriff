package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when LLM service interactions fail
 */
public class LlmServiceException extends ChatbotException {
    
    public LlmServiceException(String message) {
        super(message, "Please try again in a moment. If the problem persists, you can use the regular search features.");
    }
    
    public LlmServiceException(String message, Throwable cause) {
        super(message, "Please try again in a moment. If the problem persists, you can use the regular search features.", cause);
    }
    
    public LlmServiceException(String message, String suggestion) {
        super(message, suggestion);
    }
    
    public LlmServiceException(String message, String suggestion, Throwable cause) {
        super(message, suggestion, cause);
    }
}