package com.tariffsheriff.backend.chatbot.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiResponseTest {

    @Test
    void testOpenAiResponseGettersSetters() {
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        List<OpenAiResponse.Choice> choices = List.of(choice);
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        
        // Test no-arg constructor and setters
        OpenAiResponse resp = new OpenAiResponse();
        resp.setId("resp_123");
        resp.setObject("chat.completion");
        resp.setCreated(123456789L);
        resp.setModel("gpt-4");
        resp.setChoices(choices);
        resp.setUsage(usage);
        resp.setSystemFingerprint("fp_abc");

        // Test getters
        assertEquals("resp_123", resp.getId());
        assertEquals("chat.completion", resp.getObject());
        assertEquals(123456789L, resp.getCreated());
        assertEquals("gpt-4", resp.getModel());
        assertEquals(choices, resp.getChoices());
        assertEquals(usage, resp.getUsage());
        assertEquals("fp_abc", resp.getSystemFingerprint());
    }

    @Test
    void testChoiceGettersSetters() {
        OpenAiResponse.Message msg = new OpenAiResponse.Message();
        
        // Test no-arg constructor and setters
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(msg);
        choice.setFinishReason("stop");

        // Test getters
        assertEquals(0, choice.getIndex());
        assertEquals(msg, choice.getMessage());
        assertEquals("stop", choice.getFinishReason());
    }

    @Test
    void testMessageGettersSetters() {
        OpenAiResponse.ToolCall tc = new OpenAiResponse.ToolCall();
        List<OpenAiResponse.ToolCall> toolCalls = List.of(tc);
        
        // Test no-arg constructor and setters
        OpenAiResponse.Message msg = new OpenAiResponse.Message();
        msg.setRole("assistant");
        msg.setContent("Here is the weather...");
        msg.setToolCalls(toolCalls);

        // Test getters
        assertEquals("assistant", msg.getRole());
        assertEquals("Here is the weather...", msg.getContent());
        assertEquals(toolCalls, msg.getToolCalls());
    }

    @Test
    void testToolCallGettersSetters() {
        OpenAiResponse.Function func = new OpenAiResponse.Function();
        
        // Test no-arg constructor and setters
        OpenAiResponse.ToolCall tc = new OpenAiResponse.ToolCall();
        tc.setId("call_xyz");
        tc.setType("function");
        tc.setFunction(func);

        // Test getters
        assertEquals("call_xyz", tc.getId());
        assertEquals("function", tc.getType());
        assertEquals(func, tc.getFunction());
    }

    @Test
    void testFunctionGettersSetters() {
        // Test no-arg constructor and setters
        OpenAiResponse.Function func = new OpenAiResponse.Function();
        func.setName("get_weather");
        func.setArguments("{\"location\": \"Tokyo\"}");

        // Test getters
        assertEquals("get_weather", func.getName());
        assertEquals("{\"location\": \"Tokyo\"}", func.getArguments());
    }

    @Test
    void testUsageGettersSetters() {
        // Test no-arg constructor and setters
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(100);
        usage.setCompletionTokens(200);
        usage.setTotalTokens(300);

        // Test getters
        assertEquals(100, usage.getPromptTokens());
        assertEquals(200, usage.getCompletionTokens());
        assertEquals(300, usage.getTotalTokens());
    }
}