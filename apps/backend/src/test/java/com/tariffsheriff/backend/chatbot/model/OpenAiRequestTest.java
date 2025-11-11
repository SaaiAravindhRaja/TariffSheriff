package com.tariffsheriff.backend.chatbot.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiRequestTest {

    @Test
    void testOpenAiRequestGettersSettersAndConstructors() {
        OpenAiRequest.Message msg = new OpenAiRequest.Message("user", "Hello");
        List<OpenAiRequest.Message> messages = List.of(msg);
        List<OpenAiRequest.Tool> tools = List.of(new OpenAiRequest.Tool());

        // Test 2-arg constructor
        OpenAiRequest req1 = new OpenAiRequest("gpt-4", messages);
        assertEquals("gpt-4", req1.getModel());
        assertEquals(messages, req1.getMessages());

        // Test no-arg constructor and setters
        OpenAiRequest req2 = new OpenAiRequest();
        req2.setModel("gpt-3.5-turbo");
        req2.setMessages(messages);
        req2.setTools(tools);
        req2.setToolChoice("auto");
        req2.setTemperature(0.5);
        req2.setMaxTokens(1000);

        // Test getters
        assertEquals("gpt-3.5-turbo", req2.getModel());
        assertEquals(messages, req2.getMessages());
        assertEquals(tools, req2.getTools());
        assertEquals("auto", req2.getToolChoice());
        assertEquals(0.5, req2.getTemperature());
        assertEquals(1000, req2.getMaxTokens());
    }

    @Test
    void testMessageGettersSettersAndConstructors() {
        // Test 2-arg constructor
        OpenAiRequest.Message msg1 = new OpenAiRequest.Message("user", "Hello");
        assertEquals("user", msg1.getRole());
        assertEquals("Hello", msg1.getContent());

        // Test no-arg constructor and setters
        OpenAiRequest.ToolCall tc = new OpenAiRequest.ToolCall();
        List<OpenAiRequest.ToolCall> toolCalls = List.of(tc);
        
        OpenAiRequest.Message msg2 = new OpenAiRequest.Message();
        msg2.setRole("assistant");
        msg2.setContent("How can I help?");
        msg2.setToolCalls(toolCalls);
        msg2.setToolCallId("call_123");

        // Test getters
        assertEquals("assistant", msg2.getRole());
        assertEquals("How can I help?", msg2.getContent());
        assertEquals(toolCalls, msg2.getToolCalls());
        assertEquals("call_123", msg2.getToolCallId());
    }

    @Test
    void testToolCallGettersSetters() {
        OpenAiRequest.Function func = new OpenAiRequest.Function("get_weather", "{\"location\": \"Boston\"}");
        
        // Test no-arg constructor and setters
        OpenAiRequest.ToolCall tc = new OpenAiRequest.ToolCall();
        tc.setId("call_abc");
        tc.setType("function");
        tc.setFunction(func);

        // Test getters
        assertEquals("call_abc", tc.getId());
        assertEquals("function", tc.getType());
        assertEquals(func, tc.getFunction());
    }

    @Test
    void testFunctionGettersSettersAndConstructors() {
        // Test 2-arg constructor
        OpenAiRequest.Function func1 = new OpenAiRequest.Function("get_weather", "{\"location\": \"Boston\"}");
        assertEquals("get_weather", func1.getName());
        assertEquals("{\"location\": \"Boston\"}", func1.getArguments());

        // Test no-arg constructor and setters
        OpenAiRequest.Function func2 = new OpenAiRequest.Function();
        func2.setName("send_email");
        func2.setArguments("{\"to\": \"test@example.com\"}");

        // Test getters
        assertEquals("send_email", func2.getName());
        assertEquals("{\"to\": \"test@example.com\"}", func2.getArguments());
    }

    @Test
    void testToolGettersSettersAndConstructors() {
        OpenAiRequest.FunctionDefinition funcDef = new OpenAiRequest.FunctionDefinition();
        
        // Test 1-arg constructor
        OpenAiRequest.Tool tool1 = new OpenAiRequest.Tool(funcDef);
        assertEquals("function", tool1.getType()); // Constructor should default type
        assertEquals(funcDef, tool1.getFunction());

        // Test no-arg constructor and setters
        OpenAiRequest.Tool tool2 = new OpenAiRequest.Tool();
        tool2.setType("custom");
        tool2.setFunction(funcDef);

        // Test getters
        assertEquals("custom", tool2.getType());
        assertEquals(funcDef, tool2.getFunction());
    }

    @Test
    void testFunctionDefinitionGettersSettersAndConstructors() {
        Map<String, Object> params = Map.of("type", "string");
        
        // Test 3-arg constructor
        OpenAiRequest.FunctionDefinition def1 = new OpenAiRequest.FunctionDefinition("get_weather", "Gets weather", params);
        assertEquals("get_weather", def1.getName());
        assertEquals("Gets weather", def1.getDescription());
        assertEquals(params, def1.getParameters());

        // Test no-arg constructor and setters
        OpenAiRequest.FunctionDefinition def2 = new OpenAiRequest.FunctionDefinition();
        def2.setName("send_email");
        def2.setDescription("Sends an email");
        def2.setParameters(params);

        // Test getters
        assertEquals("send_email", def2.getName());
        assertEquals("Sends an email", def2.getDescription());
        assertEquals(params, def2.getParameters());
    }
}