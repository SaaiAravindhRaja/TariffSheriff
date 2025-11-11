package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolDefinitionTest {

    @Test
    void defaultConstructor_setsDefaults() {
        ToolDefinition td = new ToolDefinition();
        assertTrue(td.isEnabled(), "default enabled should be true");
        assertEquals(10000, td.getTimeoutMs(), "default timeout should be 10_000ms");
    }

    @Test
    void argConstructor_andGettersSetters_work() {
        Map<String, Object> params = Map.of("key", "value");
        ToolDefinition td = new ToolDefinition("tool", "desc", params);

        assertEquals("tool", td.getName());
        assertEquals("desc", td.getDescription());
        assertEquals(params, td.getParameters());

        // mutation via setters
        td.setName("other");
        td.setDescription("other desc");
        td.setEnabled(false);
        td.setTimeoutMs(5000);

        assertEquals("other", td.getName());
        assertEquals("other desc", td.getDescription());
        assertFalse(td.isEnabled());
        assertEquals(5000, td.getTimeoutMs());
    }
}
