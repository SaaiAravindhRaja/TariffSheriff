package com.tariffsheriff.backend.chatbot.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatbotExceptionTest {

    @Test
    void testConstructors() {
        Throwable cause = new RuntimeException("root cause");

        // Test (String message)
        ChatbotException ex1 = new ChatbotException("Connection timeout");
        assertEquals("Connection timeout", ex1.getMessage());
        assertEquals("I'm having trouble connecting to my services right now. Please try again in a moment.", ex1.getUserFriendlyMessage());
        assertNull(ex1.getSuggestion());
        assertNull(ex1.getCause());

        // Test (String message, Throwable cause)
        ChatbotException ex2 = new ChatbotException("A network error", cause);
        assertEquals("A network error", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
        assertEquals("I'm having trouble connecting to my services right now. Please try again in a moment.", ex2.getUserFriendlyMessage());

        // Test (String message, String suggestion)
        ChatbotException ex3 = new ChatbotException("Rate limit exceeded", "Try again later");
        assertEquals("Rate limit exceeded", ex3.getMessage());
        assertEquals("I'm receiving a lot of requests right now. Please wait a moment and try again.", ex3.getUserFriendlyMessage());
        assertEquals("Try again later", ex3.getSuggestion());

        // Test (String message, String suggestion, Throwable cause)
        ChatbotException ex4 = new ChatbotException("Info not found", "Check spelling", cause);
        assertEquals("Info not found", ex4.getMessage());
        assertEquals("I couldn't find the information you're looking for. It might not be available in our database.", ex4.getUserFriendlyMessage());
        assertEquals("Check spelling", ex4.getSuggestion());
        assertEquals(cause, ex4.getCause());
    }

    @Test
    void testSetters() {
        ChatbotException ex = new ChatbotException("Initial message");
        
        ex.setSuggestion("New suggestion");
        ex.setUserFriendlyMessage("New friendly message");

        assertEquals("New suggestion", ex.getSuggestion());
        assertEquals("New friendly message", ex.getUserFriendlyMessage());
    }

    @Test
    void testMakeUserFriendlyLogic() {
        // Test null case
        assertEquals("I encountered an issue while processing your request. Please try again.", new ChatbotException(null).getUserFriendlyMessage());
        
        // Test connection branch
        assertEquals("I'm having trouble connecting to my services right now. Please try again in a moment.", new ChatbotException("A network error").getUserFriendlyMessage());
        
        // Test not found branch
        assertEquals("I couldn't find the information you're looking for. It might not be available in our database.", new ChatbotException("Item missing").getUserFriendlyMessage());

        // Test validation branch
        assertEquals("I had trouble understanding your request. Could you please rephrase it with more details?", new ChatbotException("Invalid format").getUserFriendlyMessage());

        // Test rate limit branch
        assertEquals("I'm receiving a lot of requests right now. Please wait a moment and try again.", new ChatbotException("Quota exceeded").getUserFriendlyMessage());

        // Test default fallback
        assertEquals("I encountered an issue while processing your request. Please try again or rephrase your question.", new ChatbotException("Some unknown error").getUserFriendlyMessage());
    }
}