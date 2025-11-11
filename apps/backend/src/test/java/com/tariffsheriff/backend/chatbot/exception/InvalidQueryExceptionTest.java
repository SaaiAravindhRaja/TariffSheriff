package com.tariffsheriff.backend.chatbot.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidQueryExceptionTest {

    @Test
    void testConstructors() {
        Throwable cause = new RuntimeException("root cause");

        // Test (String message)
        InvalidQueryException ex1 = new InvalidQueryException("Query is empty");
        assertEquals("I didn't receive a question. Please type your question and try again.", ex1.getUserFriendlyMessage());
        assertTrue(ex1.getSuggestion().contains("What's the tariff for importing coffee"));
        assertNull(ex1.getCause());

        // Test (String message, String suggestion)
        InvalidQueryException ex2 = new InvalidQueryException("Query too long", "Custom suggestion");
        assertEquals("Your question is too long. Please try breaking it into smaller, more specific questions.", ex2.getUserFriendlyMessage());
        assertEquals("Custom suggestion", ex2.getSuggestion());
        assertNull(ex2.getCause());

        // Test (String message, Throwable cause)
        InvalidQueryException ex3 = new InvalidQueryException("Contains special characters", cause);
        assertEquals("Your question contains characters I can't process. Please use standard text.", ex3.getUserFriendlyMessage());
        assertTrue(ex3.getSuggestion().contains("How to ask better questions")); // default suggestion
        assertEquals(cause, ex3.getCause());

        // Test (String message, String suggestion, Throwable cause)
        InvalidQueryException ex4 = new InvalidQueryException("Bad structure", "Custom suggestion 2", cause);
        assertEquals("I had trouble understanding the format of your question.", ex4.getUserFriendlyMessage());
        assertEquals("Custom suggestion 2", ex4.getSuggestion());
        assertEquals(cause, ex4.getCause());
    }

    @Test
    void testMakeUserFriendlyLogic() {
        // Test null
        assertEquals("I had trouble understanding your question.", new InvalidQueryException((String)null).getUserFriendlyMessage());

        // Test empty/blank
        assertEquals("I didn't receive a question. Please type your question and try again.", new InvalidQueryException("is blank").getUserFriendlyMessage());

        // Test too long
        assertEquals("Your question is too long. Please try breaking it into smaller, more specific questions.", new InvalidQueryException("exceeds length").getUserFriendlyMessage());
        
        // Test special character
        assertEquals("Your question contains characters I can't process. Please use standard text.", new InvalidQueryException("invalid character").getUserFriendlyMessage());

        // Test format
        assertEquals("I had trouble understanding the format of your question.", new InvalidQueryException("bad format").getUserFriendlyMessage());

        // Test default
        assertEquals("I had trouble understanding your question. Could you rephrase it?", new InvalidQueryException("some other validation").getUserFriendlyMessage());
    }

    @Test
    void testGenerateHelpfulSuggestionLogic() {
        // Test null
        assertTrue(new InvalidQueryException((String)null).getSuggestion().contains("How to ask better questions"));

        // Test empty/blank
        assertTrue(new InvalidQueryException("is empty").getSuggestion().contains("What's the tariff for importing coffee"));

        // Test too long
        assertTrue(new InvalidQueryException("Query is too long").getSuggestion().contains("Tips for better questions"));

        // Test default
        assertTrue(new InvalidQueryException("Some other error").getSuggestion().contains("How to ask better questions"));
    }
}