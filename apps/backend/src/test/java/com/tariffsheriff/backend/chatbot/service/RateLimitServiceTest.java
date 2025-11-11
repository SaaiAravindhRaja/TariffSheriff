package com.tariffsheriff.backend.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for RateLimitService.
 * This class has no dependencies, so it is tested as a POJO.
 */
class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private final String user1 = "user1@example.com";
    private final String user2 = "user2@example.com";

    // Get constants from the class
    // Note: If these constants change in the service, this test will fail.
    // For a more robust (but complex) test, you could use reflection to read them.
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_REQUESTS_PER_HOUR = 100;

    @BeforeEach
    void setUp() {
        // Create a new service for each test to ensure a clean state
        rateLimitService = new RateLimitService();
    }

    @Test
    void isAllowed_shouldReturnFalse_forNullOrEmptyUser() {
        assertFalse(rateLimitService.isAllowed(null));
        assertFalse(rateLimitService.isAllowed(""));
        assertEquals(0, rateLimitService.getTrackedUsersCount());
    }

    @Test
    void isAllowed_shouldReturnTrue_forFirstRequest() {
        assertTrue(rateLimitService.isAllowed(user1));
        assertEquals(1, rateLimitService.getTrackedUsersCount());
    }

    @Test
    void isAllowed_shouldEnforceMinuteLimit() {
        // --- Act ---
        // Consume all 20 minute-tokens
        for (int i = 0; i < MAX_REQUESTS_PER_MINUTE; i++) {
            assertTrue(rateLimitService.isAllowed(user1), "Request " + (i + 1) + " should be allowed");
        }

        // --- Assert ---
        // The 21st request should be denied
        assertFalse(rateLimitService.isAllowed(user1), "The 21st request should be denied");

        // Check status
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(user1);
        assertEquals(MAX_REQUESTS_PER_MINUTE, status.getRequestsInLastMinute());
        assertEquals(MAX_REQUESTS_PER_HOUR - MAX_REQUESTS_PER_MINUTE, status.getHourRemainingRequests());
    }

    

    @Test
    void isAllowed_shouldTrackUsersIndependently() {
        // --- Arrange ---
        // Exhaust user1's minute limit
        for (int i = 0; i < MAX_REQUESTS_PER_MINUTE; i++) {
            assertTrue(rateLimitService.isAllowed(user1));
        }

        // --- Act & Assert ---
        assertFalse(rateLimitService.isAllowed(user1), "User1 should be rate limited");
        assertTrue(rateLimitService.isAllowed(user2), "User2 should not be affected");
        
        assertEquals(2, rateLimitService.getTrackedUsersCount());
    }

    @Test
    void getRateLimitStatus_shouldReturnCorrectStatus() {
        // --- Arrange ---
        // New user should have full tokens (0 used)
        RateLimitService.RateLimitStatus statusNew = rateLimitService.getRateLimitStatus("new@example.com");
        assertEquals(0, statusNew.getRequestsInLastMinute());
        assertEquals(0, statusNew.getRequestsInLastHour());
        assertEquals(MAX_REQUESTS_PER_MINUTE, statusNew.getMinuteRemainingRequests());
        assertEquals(MAX_REQUESTS_PER_HOUR, statusNew.getHourRemainingRequests());
        assertFalse(statusNew.isMinuteLimitExceeded());
        assertFalse(statusNew.isHourLimitExceeded());

        // --- Act ---
        // Use 5 tokens
        for (int i = 0; i < 5; i++) {
            rateLimitService.isAllowed(user1);
        }

        // --- Assert ---
        RateLimitService.RateLimitStatus statusUsed = rateLimitService.getRateLimitStatus(user1);
        assertEquals(5, statusUsed.getRequestsInLastMinute());
        assertEquals(5, statusUsed.getRequestsInLastHour());
        assertEquals(MAX_REQUESTS_PER_MINUTE - 5, statusUsed.getMinuteRemainingRequests());
        assertEquals(MAX_REQUESTS_PER_HOUR - 5, statusUsed.getHourRemainingRequests());
    }

    @Test
    void clearUserRateLimit_shouldRemoveUser() {
        // --- Arrange ---
        rateLimitService.isAllowed(user1);
        assertEquals(1, rateLimitService.getTrackedUsersCount());
        RateLimitService.RateLimitStatus statusBefore = rateLimitService.getRateLimitStatus(user1);
        assertEquals(1, statusBefore.getRequestsInLastMinute());

        // --- Act ---
        rateLimitService.clearUserRateLimit(user1);

        // --- Assert ---
        assertEquals(0, rateLimitService.getTrackedUsersCount());
        // Getting status again will create a new, fresh bucket
        RateLimitService.RateLimitStatus statusAfter = rateLimitService.getRateLimitStatus(user1);
        assertEquals(0, statusAfter.getRequestsInLastMinute());
    }

   @Test
void cleanupOldEntries_shouldRemoveInactiveUsers() {
    // --- Arrange ---
    // 1. Add an "active" user who has used one token
    rateLimitService.isAllowed(user1); 
    
    // 2. Add a second user, who we will make "inactive"
    rateLimitService.isAllowed(user2);
    
    // At this point, both users are in the map
    assertEquals(2, rateLimitService.getTrackedUsersCount());

    // --- THIS IS THE FIX ---
    // 3. Manually make user2 "inactive" by setting their tokens back to full.
    // We get the map and the bucket as generic Objects because TokenBucket is private.
    @SuppressWarnings("unchecked")
    ConcurrentMap<String, Object> userLimiters = 
        (ConcurrentMap<String, Object>) ReflectionTestUtils.getField(rateLimitService, "userLimiters");
    
    // Get the private TokenBucket instance as a generic Object
    Object user2Bucket = userLimiters.get(user2); 
    
    // ReflectionTestUtils can still set private fields on this generic Object
    ReflectionTestUtils.setField(user2Bucket, "minuteTokens", MAX_REQUESTS_PER_MINUTE);
    ReflectionTestUtils.setField(user2Bucket, "hourTokens", MAX_REQUESTS_PER_HOUR);
    // --- END FIX ---

    // --- Act ---
    // Cleanup should call refill() and remove user2 (who is now full)
    // but keep user1 (who is not full).
    rateLimitService.cleanupOldEntries();

    // --- Assert ---
    // The inactive user (user2) should be removed
    assertEquals(1, rateLimitService.getTrackedUsersCount());
    
    // Check that user1 (active) is still present
    RateLimitService.RateLimitStatus statusUser1 = rateLimitService.getRateLimitStatus(user1);
    assertEquals(1, statusUser1.getRequestsInLastMinute());
}
}