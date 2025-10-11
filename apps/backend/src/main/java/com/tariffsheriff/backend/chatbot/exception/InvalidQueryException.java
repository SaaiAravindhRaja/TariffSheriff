package com.tariffsheriff.backend.chatbot.exception;

/**
 * Exception thrown when user query is invalid or cannot be processed
 */
public class InvalidQueryException extends ChatbotException {
    
    public InvalidQueryException(String message) {
        super(message, "Please try rephrasing your question with more specific details.");
    }
    
    public InvalidQueryException(String message, String suggestion) {
        super(message, suggestion);
    }
    
    public InvalidQueryException(String message, Throwable cause) {
        super(message, "Please try rephrasing your question with more specific details.", cause);
    }
    
    public InvalidQueryException(String message, String suggestion, Throwable cause) {
        super(message, suggestion, cause);
    }
}