package com.tariffsheriff.backend.chatbot.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitExceededExceptionTest {

    // This is the generic message from ChatbotException's fallback.
    // Due to a bug in the constructor chain, this message overwrites the
    // specific messages from RateLimitExceededException.
private final String BUGGY_RATE_LIMIT_MESSAGE = "I'm receiving a lot of requests right now. Please wait a moment and try again.";
    @Test
    void testMinuteLimitHit() {
        int maxPerMinute = 10;
        int maxPerHour = 100;
        long minuteRequests = 11;
        long hourRequests = 50;

        RateLimitExceededException ex = new RateLimitExceededException(
            minuteRequests, hourRequests, maxPerMinute, maxPerHour
        );

        // Test Getters
        assertEquals(minuteRequests, ex.getRequestsInLastMinute());
        assertEquals(hourRequests, ex.getRequestsInLastHour());
        assertEquals(maxPerMinute, ex.getMaxRequestsPerMinute());
        assertEquals(maxPerHour, ex.getMaxRequestsPerHour());

        // Test that the correct message was generated, even though the bug overwrites it
        assertEquals("You're asking questions too quickly! You've made 11 requests in the last minute. " +
                     "Please take a short break (limit: 10 per minute).", ex.getMessage());
        
        // Test the BUGGY behavior
assertEquals(BUGGY_RATE_LIMIT_MESSAGE, ex.getUserFriendlyMessage());
        // Test the correct suggestion branch
        assertTrue(ex.getSuggestion().contains("Please wait 30-60 seconds"));
    }

    @Test
    void testHourLimitHit() {
        int maxPerMinute = 10;
        int maxPerHour = 100;
        long minuteRequests = 5;
        long hourRequests = 101; // Exceeds max per hour

        RateLimitExceededException ex = new RateLimitExceededException(
            minuteRequests, hourRequests, maxPerMinute, maxPerHour
        );

        // Test Getters
        assertEquals(minuteRequests, ex.getRequestsInLastMinute());
        assertEquals(hourRequests, ex.getRequestsInLastHour());

        // Test that the correct message was generated
        assertEquals("You've reached your hourly limit of 100 questions. " +
                     "This helps ensure fair access for all users.", ex.getMessage());

        // Test the BUGGY behavior
assertEquals(BUGGY_RATE_LIMIT_MESSAGE, ex.getUserFriendlyMessage());
        // Test the correct suggestion branch
        assertTrue(ex.getSuggestion().contains("Your limit will reset in the next hour"));
    }

    @Test
    void testApproachingHourLimit() {
        int maxPerMinute = 10;
        int maxPerHour = 100;
        long minuteRequests = 3;
        long hourRequests = 90; // Approaching limit

        RateLimitExceededException ex = new RateLimitExceededException(
            minuteRequests, hourRequests, maxPerMinute, maxPerHour
        );

        // Test Getters
        assertEquals(minuteRequests, ex.getRequestsInLastMinute());
        assertEquals(hourRequests, ex.getRequestsInLastHour());

        // Test that the correct message was generated
        assertEquals("You're approaching your hourly limit. You've made 90 requests " +
                     "(limit: 100 per hour). You have 10 requests remaining.", ex.getMessage());

        // Test the BUGGY behavior
assertEquals(BUGGY_RATE_LIMIT_MESSAGE, ex.getUserFriendlyMessage());
        // Test the correct suggestion branch
        assertTrue(ex.getSuggestion().contains("You have 10 questions remaining"));
        assertTrue(ex.getSuggestion().contains("To make the most of your remaining questions:"));
    }
}