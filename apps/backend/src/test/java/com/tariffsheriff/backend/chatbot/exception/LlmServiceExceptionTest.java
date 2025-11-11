package com.tariffsheriff.backend.chatbot.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LlmServiceExceptionTest {

    @Test
    void testConstructors() {
        Throwable cause = new RuntimeException("root cause");
        String defaultSuggestion = "\n\n**While I'm recovering, you can:**\n";

        // Test (String message)
        LlmServiceException ex1 = new LlmServiceException("Authentication failed");
        assertEquals("I'm experiencing a configuration issue. Our team has been notified.", ex1.getUserFriendlyMessage());
        assertTrue(ex1.getSuggestion().startsWith(defaultSuggestion));
        assertNull(ex1.getCause());

        // Test (String message, Throwable cause)
        LlmServiceException ex2 = new LlmServiceException("Rate limit exceeded", cause);
        assertEquals("I'm receiving a high volume of requests. Please wait a moment.", ex2.getUserFriendlyMessage());
        assertTrue(ex2.getSuggestion().startsWith(defaultSuggestion));
        assertEquals(cause, ex2.getCause());

        // Test (String message, String suggestion)
        LlmServiceException ex3 = new LlmServiceException("Request timeout", "Custom suggestion");
        assertEquals("My response is taking longer than expected. Let me try again.", ex3.getUserFriendlyMessage());
        assertEquals("Custom suggestion", ex3.getSuggestion());
        assertNull(ex3.getCause());

        // Test (String message, String suggestion, Throwable cause)
        LlmServiceException ex4 = new LlmServiceException("Model unavailable", "Custom suggestion 2", cause);
        assertEquals("My AI service is temporarily unavailable. Please try again shortly.", ex4.getUserFriendlyMessage());
        assertEquals("Custom suggestion 2", ex4.getSuggestion());
        assertEquals(cause, ex4.getCause());
    }

    @Test
    void testMakeUserFriendlyLogic() {
        // Test null
        assertEquals("I'm having trouble with my AI processing right now.", new LlmServiceException((String)null).getUserFriendlyMessage());

        // Test API key
        assertEquals("I'm experiencing a configuration issue. Our team has been notified.", new LlmServiceException("Invalid API key").getUserFriendlyMessage());
        
        // Test quota
        assertEquals("I'm receiving a high volume of requests. Please wait a moment.", new LlmServiceException("Quota exceeded").getUserFriendlyMessage());

        // Test timeout
        assertEquals("My response is taking longer than expected. Let me try again.", new LlmServiceException("Deadline exceeded").getUserFriendlyMessage());
        
        // Test model
        assertEquals("My AI service is temporarily unavailable. Please try again shortly.", new LlmServiceException("The model is unavailable").getUserFriendlyMessage());

        // Test default
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", new LlmServiceException("Some other LLM error").getUserFriendlyMessage());
    }
}