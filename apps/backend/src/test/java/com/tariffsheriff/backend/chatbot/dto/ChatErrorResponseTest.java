package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ChatErrorResponseTest {

    @Test
    void defaultConstructor_setsTimestamp() {
        ChatErrorResponse dto = new ChatErrorResponse();
        assertNotNull(dto.getTimestamp());
        assertTrue(dto.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ChatErrorResponse dto = new ChatErrorResponse();
        dto.setError("ValidationError");
        dto.setMessage("Invalid query");
        dto.setSuggestion("Try simplifying your input");
        dto.setConversationId("abc123");

        assertEquals("ValidationError", dto.getError());
        assertEquals("Invalid query", dto.getMessage());
        assertEquals("Try simplifying your input", dto.getSuggestion());
        assertEquals("abc123", dto.getConversationId());
    }

    @Test
    void constructor_withErrorAndMessage_setsValues() {
        ChatErrorResponse dto = new ChatErrorResponse("BadRequest", "Invalid query");
        assertEquals("BadRequest", dto.getError());
        assertEquals("Invalid query", dto.getMessage());
        assertNotNull(dto.getTimestamp());
    }
    @Test
    void constructor_withErrorAndMessageAndSuggestion_setsValues() {
        ChatErrorResponse dto = new ChatErrorResponse("BadRequest", "Invalid query", "Try again");
        assertEquals("BadRequest", dto.getError());
        assertEquals("Invalid query", dto.getMessage());
        assertEquals("Try again", dto.getSuggestion());
        assertNotNull(dto.getTimestamp());
    }
}
