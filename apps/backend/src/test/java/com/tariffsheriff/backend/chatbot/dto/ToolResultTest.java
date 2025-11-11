package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolResultTest {

    @Test
    void testFactoryMethods() {
        // Test .success()
        ToolResult res1 = ToolResult.success("toolA", "Success data");
        assertTrue(res1.isSuccess());
        assertEquals("toolA", res1.getToolName());
        assertEquals("Success data", res1.getData());
        assertNull(res1.getError());

        // Test .error()
        ToolResult res2 = ToolResult.error("toolB", "Error message");
        assertFalse(res2.isSuccess());
        assertEquals("toolB", res2.getToolName());
        assertEquals("Error message", res2.getError());
        assertNull(res2.getData());
    }

    @Test
    void testConstructors() {
        // Test no-arg constructor
        ToolResult res1 = new ToolResult();
        assertFalse(res1.isSuccess()); // Default boolean
        assertNull(res1.getData());
        
        // Test 2-arg constructor
        ToolResult res2 = new ToolResult(true, "Data");
        assertTrue(res2.isSuccess());
        assertEquals("Data", res2.getData());
        assertNull(res2.getToolName());

        // Test 3-arg constructor
        ToolResult res3 = new ToolResult("toolC", false, "No data");
        assertFalse(res3.isSuccess());
        assertEquals("toolC", res3.getToolName());
        assertEquals("No data", res3.getData());

        // Test 5-arg constructor
        ToolResult res4 = new ToolResult("toolD", true, "Full data", "No error", 150L);
        assertTrue(res4.isSuccess());
        assertEquals("toolD", res4.getToolName());
        assertEquals("Full data", res4.getData());
        assertEquals("No error", res4.getError());
        assertEquals(150L, res4.getExecutionTimeMs());
    }

    @Test
    void testSettersAndGetters() {
        ToolResult res = new ToolResult();
        
        res.setSuccess(true);
        res.setData("Some data");
        res.setError("Some error");
        res.setToolName("setterTool");
        res.setExecutionTimeMs(123L);

        assertTrue(res.isSuccess());
        assertEquals("Some data", res.getData());
        assertEquals("Some error", res.getError());
        assertEquals("setterTool", res.getToolName());
        assertEquals(123L, res.getExecutionTimeMs());
    }
}