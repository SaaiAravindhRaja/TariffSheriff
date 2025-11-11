package com.tariffsheriff.backend.chatbot.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ToolCallTest {

    private ToolCall toolCall;

    @BeforeEach
    void setUp() {
        // Set up a standard tool call for most tests
        Map<String, Object> args = Map.of(
                "stringArg", "hello",
                "intArg", 123,
                "doubleArg", 45.67,
                "bigDecimalArg", new BigDecimal("123.45"),
                "stringNumArg", "89.10"
        );
        toolCall = new ToolCall("testTool", args, "call_123");
    }

    @Test
    void testConstructors() {
        // Test no-arg
        ToolCall tc1 = new ToolCall();
        assertNull(tc1.getName());
        assertNull(tc1.getArguments());
        assertNull(tc1.getId());

        // Test 2-arg
        Map<String, Object> args = Map.of("key", "value");
        ToolCall tc2 = new ToolCall("toolName", args);
        assertEquals("toolName", tc2.getName());
        assertEquals(args, tc2.getArguments());
        assertNull(tc2.getId());

        // Test 3-arg
        ToolCall tc3 = new ToolCall("toolName2", args, "id_abc");
        assertEquals("toolName2", tc3.getName());
        assertEquals(args, tc3.getArguments());
        assertEquals("id_abc", tc3.getId());
    }

    @Test
    void testSettersAndGetters() {
        ToolCall tc = new ToolCall();
        Map<String, Object> args = Map.of("key", "value");

        tc.setName("setterTool");
        tc.setArguments(args);
        tc.setId("setterId");

        assertEquals("setterTool", tc.getName());
        assertEquals(args, tc.getArguments());
        assertEquals("setterId", tc.getId());
    }

    @Test
    void testGetArgumentTyped() {
        assertEquals("hello", toolCall.getArgument("stringArg", String.class));
        assertEquals(123, toolCall.getArgument("intArg", Integer.class));
        assertEquals(45.67, toolCall.getArgument("doubleArg", Double.class));
    }

    @Test
    void testGetArgumentTyped_returnsNull_whenKeyMissing() {
        assertNull(toolCall.getArgument("missingKey", String.class));
    }

    @Test
    void testGetArgumentTyped_returnsNull_whenTypeMismatch() {
        // "stringArg" is a String, not an Integer
        assertNull(toolCall.getArgument("stringArg", Integer.class));
    }

    @Test
    void testGetArgumentTyped_returnsNull_whenArgumentsAreNull() {
        ToolCall tc = new ToolCall("nullArgs", null, "id_1");
        assertNull(tc.getArgument("stringArg", String.class));
    }

    @Test
    void testGetArgumentGeneric() {
        assertEquals("hello", toolCall.getArgument("stringArg"));
        assertEquals(123, toolCall.getArgument("intArg"));
    }

    @Test
    void testGetArgumentGeneric_returnsNull_whenKeyMissing() {
        assertNull(toolCall.getArgument("missingKey"));
    }

    @Test
    void testGetArgumentGeneric_returnsNull_whenArgumentsAreNull() {
        ToolCall tc = new ToolCall("nullArgs", null, "id_1");
        assertNull(tc.getArgument("stringArg"));
    }

    @Test
    void testGetStringArgument() {
        assertEquals("hello", toolCall.getStringArgument("stringArg"));
    }

    @Test
    void testGetStringArgument_returnsNull_whenNotString() {
        assertNull(toolCall.getStringArgument("intArg"));
    }

    @Test
    void testGetStringArgumentWithDefault() {
        assertEquals("hello", toolCall.getStringArgument("stringArg", "default"));
    }

    @Test
    void testGetStringArgumentWithDefault_returnsDefault_whenKeyMissing() {
        assertEquals("default", toolCall.getStringArgument("missingKey", "default"));
    }

    @Test
    void testGetStringArgumentWithDefault_returnsDefault_whenTypeMismatch() {
        // intArg is an Integer, so getStringArgument returns null, triggering the default
        assertEquals("default", toolCall.getStringArgument("intArg", "default"));
    }

    @Test
    void testGetBigDecimalArgument_handlesAllTypes() {
        // 1. From BigDecimal
        assertEquals(0, new BigDecimal("123.45").compareTo(toolCall.getBigDecimalArgument("bigDecimalArg")));
        // 2. From Number (Integer)
        assertEquals(0, new BigDecimal("123").compareTo(toolCall.getBigDecimalArgument("intArg")));
        // 3. From Number (Double)
        assertEquals(0, new BigDecimal("45.67").compareTo(toolCall.getBigDecimalArgument("doubleArg")));
        // 4. From String
        assertEquals(0, new BigDecimal("89.10").compareTo(toolCall.getBigDecimalArgument("stringNumArg")));
    }

    @Test
    void testGetBigDecimalArgument_returnsNull_whenKeyMissing() {
        assertNull(toolCall.getBigDecimalArgument("missingKey"));
    }

    @Test
    void testGetBigDecimalArgument_returnsNull_whenArgumentsAreNull() {
        ToolCall tc = new ToolCall("nullArgs", null);
        assertNull(tc.getBigDecimalArgument("bigDecimalArg"));
    }

    @Test
    void testGetBigDecimalArgument_returnsNull_whenNotANumber() {
        // "stringArg" is "hello", which cannot be parsed
        assertNull(toolCall.getBigDecimalArgument("stringArg"));
    }

    @Test
    void testGetBigDecimalArgument_returnsNull_whenInvalidString() {
        ToolCall tc = new ToolCall("invalidString", Map.of("badString", "not-a-number"));
        assertNull(tc.getBigDecimalArgument("badString"));
    }
    
    @Test
    void testGetBigDecimalArgument_returnsNull_whenWrongType() {
        // Test a type that is not a Number, String, or BigDecimal
        ToolCall tc = new ToolCall("wrongType", Map.of("list", Collections.emptyList()));
        assertNull(tc.getBigDecimalArgument("list"));
    }

    @Test
    void testStaticConstants() {
        assertEquals("__direct_text_response", ToolCall.DIRECT_RESPONSE_TOOL);
        assertEquals("direct_response", ToolCall.getDirectResponseToolLabel());
    }
}