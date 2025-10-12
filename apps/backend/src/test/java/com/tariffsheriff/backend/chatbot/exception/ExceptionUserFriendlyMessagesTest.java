package com.tariffsheriff.backend.chatbot.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test user-friendly error messages in exceptions
 */
class ExceptionUserFriendlyMessagesTest {

    @Test
    void testChatbotException_UserFriendlyMessage() {
        ChatbotException ex = new ChatbotException("Connection timeout occurred");
        
        assertNotNull(ex.getUserFriendlyMessage());
        assertFalse(ex.getUserFriendlyMessage().contains("timeout"));
        assertTrue(ex.getUserFriendlyMessage().toLowerCase().contains("trouble connecting") ||
                   ex.getUserFriendlyMessage().toLowerCase().contains("connecting"));
    }

    @Test
    void testChatbotException_NullPointerMessage() {
        ChatbotException ex = new ChatbotException("NullPointerException: value is null");
        
        assertNotNull(ex.getUserFriendlyMessage());
        assertFalse(ex.getUserFriendlyMessage().contains("NullPointer"));
        assertTrue(ex.getUserFriendlyMessage().toLowerCase().contains("couldn't find") ||
                   ex.getUserFriendlyMessage().toLowerCase().contains("not available"));
    }

    @Test
    void testLlmServiceException_UserFriendlyMessage() {
        LlmServiceException ex = new LlmServiceException("API key authentication failed");
        
        assertNotNull(ex.getMessage());
        assertNotNull(ex.getSuggestion());
        assertFalse(ex.getMessage().contains("API key"));
        assertTrue(ex.getMessage().toLowerCase().contains("configuration") ||
                   ex.getMessage().toLowerCase().contains("issue"));
        assertTrue(ex.getSuggestion().contains("Calculator") || 
                   ex.getSuggestion().contains("Database"));
    }

    @Test
    void testLlmServiceException_WithSuggestions() {
        LlmServiceException ex = new LlmServiceException("Model unavailable");
        
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getSuggestion().contains("http"));
        assertTrue(ex.getSuggestion().contains("trade.gov") || 
                   ex.getSuggestion().contains("wto.org"));
    }

    @Test
    void testToolExecutionException_TariffTool() {
        ToolExecutionException ex = new ToolExecutionException(
                "TariffLookupTool", "Database query failed");
        
        assertNotNull(ex.getUserFriendlyMessage());
        assertNotNull(ex.getSuggestion());
        // Verify technical details are hidden
        assertFalse(ex.getUserFriendlyMessage().contains("Database query"));
        // Verify message is user-friendly (not empty and reasonable length)
        assertTrue(ex.getUserFriendlyMessage().length() > 10);
        // Verify suggestion is not empty and contains helpful information
        String suggestion = ex.getSuggestion();
        assertFalse(suggestion.isEmpty());
        assertTrue(suggestion.length() > 50); // Should have substantial content
    }

    @Test
    void testToolExecutionException_HsCodeTool() {
        ToolExecutionException ex = new ToolExecutionException(
                "HsCodeFinderTool", "Product not found");
        
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getSuggestion().contains("Database"));
        assertTrue(ex.getSuggestion().contains("material"));
        assertTrue(ex.getSuggestion().contains("hts.usitc.gov"));
    }

    @Test
    void testToolExecutionException_AgreementTool() {
        ToolExecutionException ex = new ToolExecutionException(
                "AgreementTool", "Country not found");
        
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getSuggestion().contains("spelled correctly"));
        assertTrue(ex.getSuggestion().contains("wto.org") || 
                   ex.getSuggestion().contains("ustr.gov"));
    }

    @Test
    void testInvalidQueryException_EmptyQuery() {
        InvalidQueryException ex = new InvalidQueryException("Query is empty");
        
        assertNotNull(ex.getUserFriendlyMessage());
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getUserFriendlyMessage().toLowerCase().contains("didn't receive") ||
                   ex.getUserFriendlyMessage().toLowerCase().contains("question"));
        assertTrue(ex.getSuggestion().contains("Try asking"));
    }

    @Test
    void testInvalidQueryException_TooLong() {
        InvalidQueryException ex = new InvalidQueryException("Query exceeds maximum length");
        
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getSuggestion().toLowerCase().contains("break") ||
                   ex.getSuggestion().toLowerCase().contains("smaller") ||
                   ex.getSuggestion().toLowerCase().contains("specific"));
    }

    @Test
    void testRateLimitExceededException_PerMinute() {
        RateLimitExceededException ex = new RateLimitExceededException(
                15, 50, 10, 100);
        
        assertNotNull(ex.getMessage());
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getMessage().contains("minute"));
        assertTrue(ex.getSuggestion().contains("Calculator"));
        assertTrue(ex.getSuggestion().contains("no limits"));
        assertTrue(ex.getSuggestion().contains("http"));
    }

    @Test
    void testRateLimitExceededException_PerHour() {
        RateLimitExceededException ex = new RateLimitExceededException(
                5, 105, 10, 100);
        
        assertNotNull(ex.getMessage());
        assertNotNull(ex.getSuggestion());
        assertTrue(ex.getMessage().contains("hour"));
        assertTrue(ex.getSuggestion().contains("Database"));
        assertTrue(ex.getSuggestion().contains("Analytics"));
    }

    @Test
    void testRateLimitExceededException_HasOfficialResources() {
        RateLimitExceededException ex = new RateLimitExceededException(
                15, 50, 10, 100);
        
        String suggestion = ex.getSuggestion();
        assertTrue(suggestion.contains("wto.org") || 
                   suggestion.contains("trade.gov") ||
                   suggestion.contains("hts.usitc.gov"));
    }

    @Test
    void testAllExceptions_HaveNonNullMessages() {
        // Verify all exceptions have non-null user-friendly messages
        ChatbotException chatbot = new ChatbotException("test");
        LlmServiceException llm = new LlmServiceException("test");
        ToolExecutionException tool = new ToolExecutionException("tool", "test");
        InvalidQueryException invalid = new InvalidQueryException("test");
        RateLimitExceededException rateLimit = new RateLimitExceededException(1, 1, 10, 100);
        
        assertNotNull(chatbot.getUserFriendlyMessage());
        assertNotNull(llm.getUserFriendlyMessage());
        assertNotNull(tool.getUserFriendlyMessage());
        assertNotNull(invalid.getUserFriendlyMessage());
        assertNotNull(rateLimit.getMessage());
    }

    @Test
    void testAllExceptions_HaveSuggestions() {
        // Verify all exceptions have suggestions
        ChatbotException chatbot = new ChatbotException("test", "suggestion");
        LlmServiceException llm = new LlmServiceException("test");
        ToolExecutionException tool = new ToolExecutionException("tool", "test");
        InvalidQueryException invalid = new InvalidQueryException("test");
        RateLimitExceededException rateLimit = new RateLimitExceededException(1, 1, 10, 100);
        
        assertNotNull(chatbot.getSuggestion());
        assertNotNull(llm.getSuggestion());
        assertNotNull(tool.getSuggestion());
        assertNotNull(invalid.getSuggestion());
        assertNotNull(rateLimit.getSuggestion());
    }
}
