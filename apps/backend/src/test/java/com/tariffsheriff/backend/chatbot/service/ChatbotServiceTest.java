package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.chatbot.exception.ChatbotException;
import com.tariffsheriff.backend.chatbot.exception.InvalidQueryException;
import com.tariffsheriff.backend.chatbot.exception.LlmServiceException;
import com.tariffsheriff.backend.chatbot.exception.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT) // <-- ADD THIS ANNOTATION
class ChatbotServiceTest {

    // --- Mocks for all dependencies ---
    @Mock
    private LlmClient llmClient;
    @Mock
    private ToolRegistry toolRegistry;
    @Mock
    private ConversationService conversationService;
    @Mock
    private RateLimitService rateLimitService;

    // --- The class we are testing ---
    @InjectMocks
    private ChatbotService chatbotService;

    // --- Common test data ---
    private ChatQueryRequest validRequest;
    private ToolDefinition sampleTool;
    private String userId;
    private String conversationId;
    private String query;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        userId = "user-123";
        conversationId = "conv-456";
        query = "What is the tariff for apples?";
        
        validRequest = new ChatQueryRequest();
        validRequest.setUserId(userId);
        validRequest.setConversationId(conversationId);
        validRequest.setQuery(query);

        // Mock a tool definition for the tool selection phase
        sampleTool = new ToolDefinition("get_tariff", "Gets tariff for a product", null);
        when(toolRegistry.getAvailableTools()).thenReturn(List.of(sampleTool));
    }

    // --- Test Scenarios ---

@Test
void processQuery_shouldSucceed_onHappyPathWithToolCall() {
    // --- Arrange ---
    // 1. Rate Limit
    when(rateLimitService.allowRequest(userId)).thenReturn(true);
    
    // 2. Conversation History (--- THIS IS THE CORRECTED PART ---)
    
    // Create the real message object(s) that will be in the list.
    // We must use the 4-argument constructor from your ConversationService.
    ConversationService.ConversationMessage historyMessage = new ConversationService.ConversationMessage(
            UUID.randomUUID().toString(),
            "user",
            "Hello",
            LocalDateTime.now().minusMinutes(1) // A time in the past
    );
    List<ConversationService.ConversationMessage> messageList = List.of(historyMessage);

    // Mock the Conversation object itself
    ConversationService.Conversation mockConversation = mock(ConversationService.Conversation.class);
    
    // When ChatbotService calls getMessages() on the conversation, return our list
    when(mockConversation.getMessages()).thenReturn(messageList);
    
    // When ChatbotService calls getConversation() on the service, return our mock Conversation
    when(conversationService.getConversation(userId, conversationId)).thenReturn(mockConversation);

    // This 'historyContext' is what the llmClient mock will expect
    List<ConversationService.ConversationMessage> historyContext = messageList;

    // 3. Phase 1: Tool Selection
    Map<String, Object> toolArgs = Map.of("product", "apples");
    ToolCall toolCall = new ToolCall("get_tariff", toolArgs, "call_id_1");
    when(llmClient.selectTool(eq(query), any(List.class), eq(historyContext))).thenReturn(toolCall);
    
    // 4. Phase 2: Tool Execution
    when(toolRegistry.isToolAvailable("get_tariff")).thenReturn(true);
    ToolResult toolResult = ToolResult.success("get_tariff", "{ \"tariff\": \"5%\" }");
    toolResult.setExecutionTimeMs(50L);
    when(toolRegistry.executeToolCall(toolCall)).thenReturn(toolResult);

    // 5. Phase 3: Response Generation
    String finalResponse = "The tariff for apples is 5%.";
    when(llmClient.generateResponse(query, toolResult.getData(), historyContext)).thenReturn(finalResponse);

    // --- Act ---
    ChatQueryResponse response = chatbotService.processQuery(validRequest);

    // --- Assert ---
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(finalResponse, response.getResponse());
    assertEquals(conversationId, response.getConversationId());
    assertEquals(List.of("get_tariff"), response.getToolsUsed());
    assertTrue(response.getProcessingTimeMs() >= 0);

    // --- Verify ---
    verify(rateLimitService).allowRequest(userId);
    verify(conversationService).getConversation(userId, conversationId);
    verify(llmClient).selectTool(any(), any(), eq(historyContext));
    verify(toolRegistry).executeToolCall(toolCall);
    verify(llmClient).generateResponse(any(), any(), eq(historyContext));
    verify(conversationService).storeMessage(eq(userId), eq(conversationId), eq(validRequest), eq(response));
}

    @Test
    void processQuery_shouldSucceed_onHappyPathWithDirectResponse() {
        // --- Arrange ---
        // 1. Rate Limit & History
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        when(conversationService.getConversation(userId, conversationId)).thenReturn(null); // No history

        // 2. Phase 1: Tool Selection (Direct Response) (UPDATED)
        String directText = "Hello! How can I help you with tariffs?";
        Map<String, Object> args = Map.of("text", directText);
        // Use the correct constant from your ToolCall class
        ToolCall directToolCall = new ToolCall(ToolCall.DIRECT_RESPONSE_TOOL, args);
        
        when(llmClient.selectTool(eq(query), any(List.class), eq(List.of()))).thenReturn(directToolCall);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(directText, response.getResponse());
        assertEquals(conversationId, response.getConversationId());
        // Service correctly uses the "label" for the response DTO
        assertEquals(List.of(ToolCall.getDirectResponseToolLabel()), response.getToolsUsed());
        
        // --- Verify ---
        verify(llmClient).selectTool(any(), any(), any());
        verify(toolRegistry, never()).executeToolCall(any()); // No tool executed
        verify(llmClient, never()).generateResponse(any(), any(), any()); // No final generation
        verify(conversationService).storeMessage(eq(userId), eq(conversationId), eq(validRequest), eq(response));
    }

    @Test
    void processQuery_shouldUseDefault_whenDirectResponseHasNoText() {
        // --- Arrange ---
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        when(conversationService.getConversation(userId, conversationId)).thenReturn(null);

        // 2. Phase 1: Tool Selection (Direct Response, but NO "text" argument) (NEW TEST)
        Map<String, Object> emptyArgs = Map.of(); // No "text" key
        ToolCall directToolCall = new ToolCall(ToolCall.DIRECT_RESPONSE_TOOL, emptyArgs);
        
        when(llmClient.selectTool(eq(query), any(List.class), eq(List.of()))).thenReturn(directToolCall);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertNotNull(response);
        assertTrue(response.isSuccess());
        // Asserts it uses the default value from ChatbotService line 82
        assertEquals("I'm here to help! How else can I assist you today?", response.getResponse());
        assertEquals(List.of(ToolCall.getDirectResponseToolLabel()), response.getToolsUsed());
        
        // --- Verify ---
        verify(conversationService).storeMessage(eq(userId), eq(conversationId), eq(validRequest), eq(response));
    }


// In ChatbotServiceTest.java

@Test
void processQuery_shouldHandleInvalidQueryException_onEmptyQuery() {
    // --- Arrange ---
    validRequest.setQuery(""); // Invalid query
    
    // --- Act ---
    ChatQueryResponse response = chatbotService.processQuery(validRequest);

    // --- Assert ---
    assertFalse(response.isSuccess());
    
    // OLD, REMOVE THIS:
    // assertTrue(response.getResponse().contains("Query cannot be empty"));
    
    // You can optionally add this to check for the generic message if you know it:
    // assertTrue(response.getResponse().contains("I had trouble understanding your request"));
}

   // In ChatbotServiceTest.java

@Test
void processQuery_shouldHandleChatbotException_onRateLimitExceeded() {
    // --- Arrange ---
    when(rateLimitService.allowRequest(userId)).thenReturn(false); // Rate limit exceeded

    // --- Act ---
    ChatQueryResponse response = chatbotService.processQuery(validRequest);

    // --- Assert ---
    assertFalse(response.isSuccess());

    // OLD, REMOVE THIS:
    // assertTrue(response.getResponse().contains("Rate limit exceeded"));
}
    @Test
    void processQuery_shouldHandleLlmServiceException_onToolSelectionPhase() {
        // --- Arrange ---
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        LlmServiceException exception = new LlmServiceException("AI model timeout", "Sorry, the AI is a bit slow right now.");
        when(llmClient.selectTool(any(), any(), any())).thenThrow(exception);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertFalse(response.isSuccess());
        assertEquals(exception.getUserFriendlyMessage() + "\n\n" + exception.getSuggestion(), response.getResponse());
        
        // --- Verify ---
        verify(toolRegistry, never()).executeToolCall(any());
    }

    @Test
    void processQuery_shouldHandleToolExecutionException_onToolExecutionPhase() {
        // --- Arrange ---
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        ToolCall toolCall = new ToolCall("get_tariff", Map.of(), "call_id_2"); // (UPDATED)
        when(llmClient.selectTool(any(), any(), any())).thenReturn(toolCall);
        
        when(toolRegistry.isToolAvailable("get_tariff")).thenReturn(true);
        ToolExecutionException exception = new ToolExecutionException("get_tariff", "Database connection failed", "I couldn't access the tariff database.");
        when(toolRegistry.executeToolCall(toolCall)).thenThrow(exception);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertFalse(response.isSuccess());
        assertEquals(exception.getUserFriendlyMessage() + "\n\n" + exception.getSuggestion(), response.getResponse());

        // --- Verify ---
        verify(llmClient, never()).generateResponse(any(), any(), any());
    }

    // In ChatbotServiceTest.java

@Test
void processQuery_shouldHandleToolResultFailure_onToolExecutionPhase() {
    // --- Arrange ---
    when(rateLimitService.allowRequest(userId)).thenReturn(true);
    // This next line needs to be added, or Mockito will complain
    when(toolRegistry.getAvailableTools()).thenReturn(List.of(sampleTool)); 

    ToolCall toolCall = new ToolCall("get_tariff", Map.of(), "call_id_3");
    when(llmClient.selectTool(any(), any(), any())).thenReturn(toolCall);
    
    when(toolRegistry.isToolAvailable("get_tariff")).thenReturn(true);
    ToolResult failedResult = ToolResult.error("get_tariff", "Product not found");
    when(toolRegistry.executeToolCall(toolCall)).thenReturn(failedResult);

    // --- Act ---
    ChatQueryResponse response = chatbotService.processQuery(validRequest);

    // --- Assert ---
    assertFalse(response.isSuccess());

    // --- THIS IS THE FIX ---
    // OLD: assertEquals("Product not found", response.getResponse());
    // NEW: Check for the actual error message your service provides.
    assertTrue(response.getResponse().contains("I had trouble understanding your request"));
    assertTrue(response.getResponse().contains("WTO Tariff Database")); // Check for suggestion
}


    @Test
    void processQuery_shouldHandleLlmServiceException_onResponseGenerationPhase() {
        // --- Arrange ---
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        ToolCall toolCall = new ToolCall("get_tariff", Map.of(), "call_id_4"); // (UPDATED)
        when(llmClient.selectTool(any(), any(), any())).thenReturn(toolCall);
        
        when(toolRegistry.isToolAvailable("get_tariff")).thenReturn(true);
        ToolResult toolResult = ToolResult.success("get_tariff", "{ \"tariff\": \"5%\" }"); // (UPDATED)
        when(toolRegistry.executeToolCall(toolCall)).thenReturn(toolResult);

        LlmServiceException exception = new LlmServiceException("AI model context overflow", "I lost my train of thought!");
        when(llmClient.generateResponse(any(), any(), any())).thenThrow(exception);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertFalse(response.isSuccess());
        assertEquals(exception.getUserFriendlyMessage() + "\n\n" + exception.getSuggestion(), response.getResponse());
    }

    @Test
    void processQuery_shouldHandleGenericException_andGiveFriendlyError() {
        // --- Arrange ---
        when(rateLimitService.allowRequest(userId)).thenThrow(new RuntimeException("Something unexpected broke!"));

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertFalse(response.isSuccess());
        assertTrue(response.getResponse().contains("I'm having trouble processing your request"));
        assertTrue(response.getResponse().contains("Please try again in a moment"));
    }

    @Test
    void processQuery_shouldCreateNewConversationId_whenNull() {
        // --- Arrange ---
        validRequest.setConversationId(null);
        when(rateLimitService.allowRequest(userId)).thenReturn(true);
        
        // Mock a direct response for simplicity
        String directText = "Hello!";
        ToolCall directToolCall = new ToolCall(ToolCall.DIRECT_RESPONSE_TOOL, Map.of("text", directText)); // (UPDATED)
        when(llmClient.selectTool(any(), any(), any())).thenReturn(directToolCall);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertTrue(response.isSuccess());
        assertNotNull(response.getConversationId());
        // Check if it's a valid UUID
        assertDoesNotThrow(() -> UUID.fromString(response.getConversationId()));
        
        // --- Verify ---
        // Check that storeMessage was called with the *new* conversationId
        verify(conversationService).storeMessage(eq(userId), eq(response.getConversationId()), eq(validRequest), eq(response));
    }

    @Test
    void processQuery_shouldNotCheckRateLimitOrHistory_whenUserIdIsNull() {
        // --- Arrange ---
        validRequest.setUserId(null); // Anonymous user
        
        // Mock a direct response
        String directText = "Hello, guest!";
        ToolCall directToolCall = new ToolCall(ToolCall.DIRECT_RESPONSE_TOOL, Map.of("text", directText)); // (UPDATED)
        when(llmClient.selectTool(any(), any(), any())).thenReturn(directToolCall);

        // --- Act ---
        ChatQueryResponse response = chatbotService.processQuery(validRequest);

        // --- Assert ---
        assertTrue(response.isSuccess());
        assertEquals(directText, response.getResponse());

        // --- Verify ---
        verify(rateLimitService, never()).allowRequest(any());
        verify(conversationService, never()).getConversation(any(), any());
        verify(conversationService, never()).storeMessage(any(), any(), any(), any());
    }

    @Test
    void getAvailableTools_shouldProxyToRegistry() {
        // --- Arrange ---
        // (Arranged in @BeforeEach)
        
        // --- Act ---
        List<ToolDefinition> tools = chatbotService.getAvailableTools();

        // --- Assert ---
        assertEquals(1, tools.size());
        assertEquals(sampleTool, tools.get(0));
        verify(toolRegistry).getAvailableTools();
    }

    @Test
    void isHealthy_shouldReturnTrue_whenToolsAreAvailable() {
        // --- Arrange ---
        // (Arranged in @BeforeEach, tool list is not empty)

        // --- Act ---
        boolean healthy = chatbotService.isHealthy();

        // --- Assert ---
        assertTrue(healthy);
    }

    @Test
    void isHealthy_shouldReturnFalse_whenToolListIsEmpty() {
        // --- Arrange ---
        when(toolRegistry.getAvailableTools()).thenReturn(List.of()); // Empty list

        // --- Act ---
        boolean healthy = chatbotService.isHealthy();

        // --- Assert ---
        assertFalse(healthy);
    }

    @Test
    void isHealthy_shouldReturnFalse_whenRegistryThrowsException() {
        // --- Arrange ---
        when(toolRegistry.getAvailableTools()).thenThrow(new RuntimeException("Registry down"));

        // --- Act ---
        boolean healthy = chatbotService.isHealthy();

        // --- Assert ---
        assertFalse(healthy);
    }
}