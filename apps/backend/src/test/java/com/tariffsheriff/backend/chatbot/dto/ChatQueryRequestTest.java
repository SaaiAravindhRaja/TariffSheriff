package com.tariffsheriff.backend.chatbot.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChatQueryRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // Set up a validator instance to test the annotations
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testConstructors() {
        // Test 1-arg constructor
        ChatQueryRequest req1 = new ChatQueryRequest("Hello");
        assertEquals("Hello", req1.getQuery());
        assertNull(req1.getConversationId());
        assertNull(req1.getUserId());

        // Test 2-arg constructor
        ChatQueryRequest req2 = new ChatQueryRequest("Hello", "conv123");
        assertEquals("Hello", req2.getQuery());
        assertEquals("conv123", req2.getConversationId());
        assertNull(req2.getUserId());
        
        // Test 3-arg constructor
        ChatQueryRequest req3 = new ChatQueryRequest("Hello", "conv123", "user456");
        assertEquals("Hello", req3.getQuery());
        assertEquals("conv123", req3.getConversationId());
        assertEquals("user456", req3.getUserId());
    }

    @Test
    void testSettersAndGetters() {
        // Test no-arg constructor and setters
        ChatQueryRequest req = new ChatQueryRequest();
        req.setQuery("Test Query");
        req.setConversationId("abc");
        req.setUserId("xyz");

        assertEquals("Test Query", req.getQuery());
        assertEquals("abc", req.getConversationId());
        assertEquals("xyz", req.getUserId());
    }

    @Test
    void testValidation_queryIsValid() {
        ChatQueryRequest req = new ChatQueryRequest("This is a valid query");
        Set<ConstraintViolation<ChatQueryRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Valid query should have no violations");
    }

    @Test
    void testValidation_queryIsBlank() {
        ChatQueryRequest req = new ChatQueryRequest("   "); // Blank query
        Set<ConstraintViolation<ChatQueryRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Blank query should have a violation");
        
        ConstraintViolation<ChatQueryRequest> violation = violations.iterator().next();
        assertEquals("Query cannot be empty", violation.getMessage());
    }

    @Test
    void testValidation_queryIsNull() {
        ChatQueryRequest req = new ChatQueryRequest(); // Query is null
        Set<ConstraintViolation<ChatQueryRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Null query should have a violation");
    }

    @Test
    void testValidation_queryIsTooLong() {
        String longQuery = "a".repeat(2001); // 2001 characters
        ChatQueryRequest req = new ChatQueryRequest(longQuery);
        
        Set<ConstraintViolation<ChatQueryRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Long query should have a violation");
        
        ConstraintViolation<ChatQueryRequest> violation = violations.iterator().next();
        assertEquals("Query cannot exceed 2000 characters", violation.getMessage());
    }
}