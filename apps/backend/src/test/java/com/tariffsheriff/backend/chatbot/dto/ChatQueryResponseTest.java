package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatQueryResponseTest {

    @Test
    void testNoArgConstructor_setsDefaults() {
        ChatQueryResponse resp = new ChatQueryResponse();
        
        assertNotNull(resp.getTimestamp(), "Timestamp should be set by default");
        assertTrue(resp.isSuccess(), "Success should be true by default");
    }

    @Test
    void testOneArgConstructor() {
        ChatQueryResponse resp = new ChatQueryResponse("Test response");
        
        assertEquals("Test response", resp.getResponse());
        assertNotNull(resp.getTimestamp(), "Timestamp should be set");
        assertTrue(resp.isSuccess(), "Success should be true");
    }

    @Test
    void testTwoArgConstructor() {
        ChatQueryResponse resp = new ChatQueryResponse("Test response", "conv123");
        
        assertEquals("Test response", resp.getResponse());
        assertEquals("conv123", resp.getConversationId());
        assertNotNull(resp.getTimestamp(), "Timestamp should be set");
        assertTrue(resp.isSuccess(), "Success should be true");
    }

    @Test
    void testSettersAndGetters() {
        ChatQueryResponse resp = new ChatQueryResponse();
        
        LocalDateTime customTime = LocalDateTime.now().minusDays(1);
        List<String> tools = List.of("getTariffRate");

        resp.setResponse("Full response");
        resp.setConversationId("conv456");
        resp.setTimestamp(customTime);
        resp.setToolsUsed(tools);
        resp.setProcessingTimeMs(150L);
        resp.setSuccess(false);
        resp.setCached(true);
        resp.setDegraded(true);
        resp.setConfidence(0.95);

        assertEquals("Full response", resp.getResponse());
        assertEquals("conv456", resp.getConversationId());
        assertEquals(customTime, resp.getTimestamp());
        assertEquals(tools, resp.getToolsUsed());
        assertEquals(150L, resp.getProcessingTimeMs());
        assertFalse(resp.isSuccess());
        assertTrue(resp.isCached());
        assertTrue(resp.isDegraded());
        assertEquals(0.95, resp.getConfidence());
    }

    @Test
    void testToolResultsAliasMethods() {
        ChatQueryResponse resp = new ChatQueryResponse();
        List<String> tools = List.of("tool_alias_test");

        // Test the alias setter
        resp.setToolResults(tools);
        
        // Check that both the original getter and the alias getter work
        assertEquals(tools, resp.getToolsUsed(), "setToolResults should set toolsUsed");
        assertEquals(tools, resp.getToolResults(), "getToolResults should return toolsUsed");
    }
}