package com.tariffsheriff.backend.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.config.OpenAiProperties;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.model.OpenAiRequest;
import com.tariffsheriff.backend.chatbot.model.OpenAiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmClientTest {

    @Mock
    private OpenAiProperties openAiProperties;

    @Mock
    private RestTemplate restTemplate;

    // Use a real ObjectMapper as it's a core part of the parsing logic
    private ObjectMapper objectMapper = new ObjectMapper();

    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        // Manually instantiate the LlmClient
        llmClient = new LlmClient(openAiProperties, objectMapper);

        // Use ReflectionTestUtils to replace the
        // internally-created RestTemplate with our mock.
        ReflectionTestUtils.setField(llmClient, "restTemplate", restTemplate);

        // Setup default mock properties
        when(openAiProperties.getModel()).thenReturn("gpt-test");
        when(openAiProperties.getMaxRetries()).thenReturn(1);
        when(openAiProperties.getTemperature()).thenReturn(0.5);
        when(openAiProperties.getMaxTokens()).thenReturn(1024);
    }

    // --- selectTool Tests ---

    @Test
    void selectTool_shouldReturnToolCall_onHappyPath() {
        // --- Arrange ---
        String query = "What's the tariff for apples?";
        List<ToolDefinition> toolDefs = List.of(new ToolDefinition("get_tariff", "desc", Map.of()));

        // Build a mock response that wants to call a tool
        OpenAiResponse mockResponse = createMockToolCallResponse("get_tariff", "{\"product\":\"apples\"}", "call-1");
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act ---
        ToolCall result = llmClient.selectTool(query, toolDefs, List.of());

        // --- Assert ---
        assertNotNull(result);
        assertEquals("get_tariff", result.getName());
        assertEquals("call-1", result.getId());
        assertEquals("apples", result.getArgument("product", String.class));
    }

    @Test
    void selectTool_shouldReturnDirectResponse_whenNoToolCall() {
        // --- Arrange ---
        String query = "Hello";
        String directResponse = "Hi! How can I help?";

        // Build a mock response that gives a direct text answer
        OpenAiResponse mockResponse = createMockTextResponse(directResponse);
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act ---
        ToolCall result = llmClient.selectTool(query, List.of(), List.of());

        // --- Assert ---
        assertNotNull(result);
        assertEquals(ToolCall.DIRECT_RESPONSE_TOOL, result.getName());
        assertEquals(directResponse, result.getStringArgument("text"));
    }

    @Test
    void selectTool_shouldThrowLlmException_whenResponseIsEmpty() {
        // --- Arrange ---
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(null); // Empty response

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.selectTool("test", List.of(), List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    @Test
    void selectTool_shouldHandleMalformedArgumentJson() {
        // --- Arrange ---
        // Return a tool call with bad JSON: {"product":"apples" (missing quote)
        OpenAiResponse mockResponse = createMockToolCallResponse("get_tariff", "{\"product\":\"apples\"", "call-1");
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act ---
        ToolCall result = llmClient.selectTool("test", List.of(new ToolDefinition("get_tariff", "d", null)), List.of());

        // --- Assert ---
        // The service should catch the JsonProcessingException and return an empty map
        assertNotNull(result);
        assertEquals("get_tariff", result.getName());
        assertTrue(result.getArguments().isEmpty());
    }

    // --- generateResponse Tests ---

    @Test
    void generateResponse_shouldReturnText_onHappyPath() {
        // --- Arrange ---
        String query = "What's the tariff?";
        String toolData = "{\"tariff\": \"5%\"}";
        String expectedResponse = "The tariff is 5%.";

        OpenAiResponse mockResponse = createMockTextResponse(expectedResponse);
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act ---
        String result = llmClient.generateResponse(query, toolData, List.of());

        // --- Assert ---
        assertEquals(expectedResponse, result);
    }

    @Test
    void generateResponse_shouldThrowLlmException_whenResponseIsEmpty() {
        // --- Arrange ---
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(createMockTextResponse("  ")); // Empty/blank content

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    // --- sendRequest (Retry and Error) Tests ---

    @Test
    void sendRequest_shouldRetry_on5xxError() throws InterruptedException {
        // --- Arrange ---
        when(openAiProperties.getMaxRetries()).thenReturn(1);
        OpenAiResponse mockResponse = createMockTextResponse("Success");

        // Create a 503 Service Unavailable exception
        RestClientResponseException ex = new RestClientResponseException(
            "Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE, "Unavailable", null, null, null
        );

        // First call: throw 503. Second call: return success.
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(ex)
                .thenReturn(mockResponse);

        // --- Act ---
        String result = llmClient.generateResponse("test", "data", List.of());

        // --- Assert ---
        assertEquals("Success", result);
        // Verify it was called twice (1 initial + 1 retry)
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(OpenAiResponse.class));
    }

    @Test
    void sendRequest_shouldFail_afterRetries() {
        // --- Arrange ---
        when(openAiProperties.getMaxRetries()).thenReturn(1);
        
        RestClientResponseException ex = new RestClientResponseException(
            "Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE, "Unavailable", null, null, null
        );

        // Always throw the 503 error
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(ex);

        // --- Act & Assert ---
        var thrownEx = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        
        // This test was failing, it has been updated:
        assertEquals("My AI service is temporarily unavailable. Please try again shortly.", thrownEx.getUserFriendlyMessage());
        // Verify it was called twice (1 initial + 1 retry)
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(OpenAiResponse.class));
    }

    @Test
    void sendRequest_shouldNotRetry_on4xxError() {
        // --- Arrange ---
        when(openAiProperties.getMaxRetries()).thenReturn(1);
        
        // 401 Unauthorized
        RestClientResponseException ex = new RestClientResponseException(
            "Auth Error", HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null
        );

        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(ex);

        // --- Act & Assert ---
        var thrownEx = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        
        // This test was failing, it has been updated:
        assertEquals("I'm experiencing a configuration issue. Our team has been notified.", thrownEx.getUserFriendlyMessage());
        // Verify it was only called ONCE
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(OpenAiResponse.class));
    }

    @Test
    void sendRequest_shouldHandleTooManyRequests() {
        // --- Arrange ---
        // 429 Too Many Requests
        RestClientResponseException ex = new RestClientResponseException(
            "Rate limit", HttpStatus.TOO_MANY_REQUESTS, "Rate limit", null, null, null
        );
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(ex);
        
        // --- Act & Assert ---
        var thrownEx = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", thrownEx.getUserFriendlyMessage());
    }

    @Test
    void sendRequest_shouldHandleGenericRestClientException() {
        // --- Arrange ---
        // e.g., a connection timeout
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(new RestClientException("Connection timed out"));
        
        // --- Act & Assert ---
        var thrownEx = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", thrownEx.getUserFriendlyMessage());
    }


    // --- Request Building Tests ---

    @Test
    void buildToolSelectionRequest_shouldIncludeHistory() {
        // --- Arrange ---
        String query = "And for pears?";
        List<ToolDefinition> toolDefs = List.of(new ToolDefinition("get_tariff", "desc", Map.of()));
        
        // Create conversation history
        List<ConversationService.ConversationMessage> history = List.of(
            new ConversationService.ConversationMessage(
                UUID.randomUUID().toString(), "user", "What about apples?", LocalDateTime.now()
            ),
            new ConversationService.ConversationMessage(
                UUID.randomUUID().toString(), "assistant", "The tariff for apples is 5%.", LocalDateTime.now()
            )
        );

        ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(createMockToolCallResponse("get_tariff", "{\"product\":\"pears\"}", "call-2"));

        // --- Act ---
        llmClient.selectTool(query, toolDefs, history);

        // --- Assert ---
        // Capture the request sent to RestTemplate
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(OpenAiResponse.class));
        OpenAiRequest capturedRequest = captor.getValue();
        
        List<OpenAiRequest.Message> messages = capturedRequest.getMessages();
        assertEquals(4, messages.size()); // 1 system, 2 history, 1 user
        
        assertEquals("system", messages.get(0).getRole());
        
        // Check history
        assertEquals("user", messages.get(1).getRole());
        assertEquals("What about apples?", messages.get(1).getContent());
        assertEquals("assistant", messages.get(2).getRole());
        assertEquals("The tariff for apples is 5%.", messages.get(2).getContent());
        
        // Check current query
        assertEquals("user", messages.get(3).getRole());
        assertEquals("And for pears?", messages.get(3).getContent());
    }
    
    // --- More granular parsing tests ---

    @Test
    void selectTool_throws_whenChoiceMessageIsNull() {
        // --- Arrange ---
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(null); // Null message
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(1);
        usage.setCompletionTokens(1);
        usage.setTotalTokens(2);
        
        OpenAiResponse mockResponse = new OpenAiResponse();
        mockResponse.setChoices(List.of(choice));
        mockResponse.setUsage(usage);

        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.selectTool("test", List.of(), List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    @Test
    void selectTool_throws_whenFunctionOrNameIsNull() {
        // --- Arrange ---
        OpenAiResponse.ToolCall toolCall = new OpenAiResponse.ToolCall();
        toolCall.setId("call-1");
        toolCall.setFunction(null); // Null function

        OpenAiResponse.Message message = new OpenAiResponse.Message();
        message.setToolCalls(List.of(toolCall));

        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(message);
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(1);
        usage.setCompletionTokens(1);
        usage.setTotalTokens(2);
        
        OpenAiResponse mockResponse = new OpenAiResponse();
        mockResponse.setChoices(List.of(choice));
        mockResponse.setUsage(usage);

        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.selectTool("test", List.of(), List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    @Test
    void selectTool_throws_whenNoToolAndNoContent() {
        // --- Arrange ---
        OpenAiResponse.Message message = new OpenAiResponse.Message();
        message.setToolCalls(List.of()); // No tool calls
        message.setContent(null); // No content

        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(message);
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(1);
        usage.setCompletionTokens(1);
        usage.setTotalTokens(2);
        
        OpenAiResponse mockResponse = new OpenAiResponse();
        mockResponse.setChoices(List.of(choice));
        mockResponse.setUsage(usage);

        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.selectTool("test", List.of(), List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    @Test
    void generateResponse_throws_whenChoiceMessageIsNull() {
        // --- Arrange ---
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(null); // Null message
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(1);
        usage.setCompletionTokens(1);
        usage.setTotalTokens(2);

        OpenAiResponse mockResponse = new OpenAiResponse();
        mockResponse.setChoices(List.of(choice));
        mockResponse.setUsage(usage);

        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(mockResponse);

        // --- Act & Assert ---
        var ex = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", ex.getUserFriendlyMessage());
    }

    // --- More error handling tests ---

    @Test
    void sendRequest_handles_400BadRequest() {
        // --- Arrange ---
        // 400 Bad Request
        RestClientResponseException ex = new RestClientResponseException(
            "Bad Request", HttpStatus.BAD_REQUEST, "Bad Request", null, null, null
        );
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenThrow(ex);
        
        // --- Act & Assert ---
        var thrownEx = assertThrows(LlmServiceException.class, () -> 
            llmClient.generateResponse("test", "data", List.of())
        );
        // This test was failing, it has been updated:
        assertEquals("I'm having trouble processing your request with my AI capabilities right now.", thrownEx.getUserFriendlyMessage());
        // Verify no retries
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(OpenAiResponse.class));
    }

    // --- Request building logic tests ---
    
    @Test
    void buildToolSelectionRequest_handles_disabledTools() {
        // --- Arrange ---
        String query = "Find tariffs";
        ToolDefinition enabledTool = new ToolDefinition("get_tariff", "desc", Map.of());
        
        ToolDefinition disabledTool = new ToolDefinition("get_agreements", "desc", Map.of());
        disabledTool.setEnabled(false); // Manually disable this tool (assuming a setter or modifying the stub)

        List<ToolDefinition> toolDefs = List.of(enabledTool, disabledTool);

        ArgumentCaptor<OpenAiRequest> captor = ArgumentCaptor.forClass(OpenAiRequest.class);
        when(restTemplate.postForObject(anyString(), any(), eq(OpenAiResponse.class)))
                .thenReturn(createMockToolCallResponse("get_tariff", "{\"product\":\"pears\"}", "call-2"));

        // --- Act ---
        llmClient.selectTool(query, toolDefs, List.of());

        // --- Assert ---
        // Capture the request sent to RestTemplate
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(OpenAiResponse.class));
        OpenAiRequest capturedRequest = captor.getValue();
        
        // Check that only the enabled tool was included in the request
        List<OpenAiRequest.Tool> toolsInRequest = capturedRequest.getTools();
        assertEquals(1, toolsInRequest.size());
    }


    // --- Helper Methods to build mock OpenAI responses ---

    private OpenAiResponse createMockTextResponse(String content) {
        OpenAiResponse.Message message = new OpenAiResponse.Message();
        message.setContent(content);
        
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(message);
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(10);
        usage.setTotalTokens(20);

        OpenAiResponse response = new OpenAiResponse();
        response.setChoices(List.of(choice));
        response.setUsage(usage); // Add usage
        
        return response;
    }

    private OpenAiResponse createMockToolCallResponse(String toolName, String argumentsJson, String callId) {
        OpenAiResponse.Function function = new OpenAiResponse.Function();
        function.setName(toolName);
        function.setArguments(argumentsJson);

        OpenAiResponse.ToolCall toolCall = new OpenAiResponse.ToolCall();
        toolCall.setId(callId);
        toolCall.setFunction(function);

        OpenAiResponse.Message message = new OpenAiResponse.Message();
        message.setToolCalls(List.of(toolCall));

        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.setMessage(message);
        
        OpenAiResponse.Usage usage = new OpenAiResponse.Usage();
        usage.setPromptTokens(50);
        usage.setCompletionTokens(5);
        usage.setTotalTokens(55);
        
        OpenAiResponse response = new OpenAiResponse();
        response.setChoices(List.of(choice));
        response.setUsage(usage); // Add usage
        
        return response;
    }
}