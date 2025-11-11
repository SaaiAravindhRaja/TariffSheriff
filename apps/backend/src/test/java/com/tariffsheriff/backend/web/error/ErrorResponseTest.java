package com.tariffsheriff.backend.web.error;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void defaultConstructor_setsTimestamp() {
        ErrorResponse resp = new ErrorResponse();
        assertNotNull(resp.getTimestamp(), "timestamp should be initialized");
    }

    @Test
    void argConstructor_andGettersSetters_work() {
        ErrorResponse resp = new ErrorResponse(404, "Not Found", "Resource missing", "/api/test");

        assertEquals(404, resp.getStatus());
        assertEquals("Not Found", resp.getError());
        assertEquals("Resource missing", resp.getMessage());
        assertEquals("/api/test", resp.getPath());

        // Test setters
        resp.setStatus(500);
        resp.setError("Server Error");
        resp.setMessage("Something went wrong");
        resp.setPath("/api/other");

        assertEquals(500, resp.getStatus());
        assertEquals("Server Error", resp.getError());
        assertEquals("Something went wrong", resp.getMessage());
        assertEquals("/api/other", resp.getPath());

        // timestamp setter
        LocalDateTime now = LocalDateTime.now();
        resp.setTimestamp(now);
        assertEquals(now, resp.getTimestamp());
    }
}


