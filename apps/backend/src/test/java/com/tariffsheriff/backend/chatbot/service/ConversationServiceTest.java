package com.tariffsheriff.backend.chatbot.service;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for ConversationService.
 * This test class treats ConversationService as a stateful, in-memory repository.
 * No mocks are needed.
 */
class ConversationServiceTest {

    private ConversationService conversationService;
    private String userId;
    private String convoId;

    // We need stubs for these DTOs
    // (Assuming they have these basic setters/getters from the previous context)
    private ChatQueryRequest createRequest(String query) {
        ChatQueryRequest req = new ChatQueryRequest();
        req.setQuery(query);
        return req;
    }

    private ChatQueryResponse createResponse(String response) {
        ChatQueryResponse res = new ChatQueryResponse();
        res.setResponse(response);
        return res;
    }

    @BeforeEach
    void setUp() {
        // Create a new service for each test to ensure a clean state
        conversationService = new ConversationService();
        userId = "test-user-1";
        convoId = "test-convo-1";
    }

    @Test
    void storeMessage_shouldCreateNewConversation_andAddMessages() {
        // --- Arrange ---
        ChatQueryRequest request = createRequest("Hello");
        ChatQueryResponse response = createResponse("Hi there!");

        // --- Act ---
        conversationService.storeMessage(userId, convoId, request, response);

        // --- Assert ---
        ConversationService.Conversation conversation = conversationService.getConversation(userId, convoId);
        assertNotNull(conversation);
        assertEquals(convoId, conversation.getId());
        assertEquals(userId, conversation.getUserId());
        
        List<ConversationService.ConversationMessage> messages = conversation.getMessages();
        assertEquals(2, messages.size());
        
        assertEquals("user", messages.get(0).getRole());
        assertEquals("Hello", messages.get(0).getContent());
        
        assertEquals("assistant", messages.get(1).getRole());
        assertEquals("Hi there!", messages.get(1).getContent());
    }

    @Test
    void storeMessage_shouldAppendToExistingConversation() {
        // --- Arrange ---
        conversationService.storeMessage(userId, convoId, createRequest("First query"), createResponse("First answer"));
        
        // --- Act ---
        conversationService.storeMessage(userId, convoId, createRequest("Second query"), createResponse("Second answer"));

        // --- Assert ---
        ConversationService.Conversation conversation = conversationService.getConversation(userId, convoId);
        assertNotNull(conversation);
        
        List<ConversationService.ConversationMessage> messages = conversation.getMessages();
        assertEquals(4, messages.size()); // 2 messages from first exchange, 2 from second
        
        assertEquals("user", messages.get(2).getRole());
        assertEquals("Second query", messages.get(2).getContent());
    }

    @Test
    void getConversation_shouldReturnNull_ifUserNotFound() {
        assertNull(conversationService.getConversation("non-existent-user", convoId));
    }

    @Test
    void getConversation_shouldReturnNull_ifConversationNotFound() {
        conversationService.storeMessage(userId, convoId, createRequest("Hello"), createResponse("Hi"));
        assertNull(conversationService.getConversation(userId, "non-existent-convo"));
    }

    @Test
    void getUserConversations_shouldReturnEmptyList_forNewUser() {
        List<ConversationService.ConversationSummary> summaries = conversationService.getUserConversations(userId);
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    void getUserConversations_shouldReturnSortedByLastMessageTime() throws InterruptedException {
        // --- Arrange ---
        String convo1 = "convo-old";
        String convo2 = "convo-new";

        // Add "old" conversation
        conversationService.storeMessage(userId, convo1, createRequest("Query 1"), createResponse("Answer 1"));
        
        // Ensure a timestamp difference
        TimeUnit.MILLISECONDS.sleep(10);
        
        // Add "new" conversation
        conversationService.storeMessage(userId, convo2, createRequest("Query 2"), createResponse("Answer 2"));

        // --- Act ---
        List<ConversationService.ConversationSummary> summaries = conversationService.getUserConversations(userId);

        // --- Assert ---
        assertEquals(2, summaries.size());
        // The newest conversation (convo2) should be first in the list
        assertEquals(convo2, summaries.get(0).getId());
        assertEquals("convo-new", summaries.get(0).getId());
        assertEquals("Query 2", summaries.get(0).getTitle()); // Title comes from first user message
        
        assertEquals(convo1, summaries.get(1).getId());
        assertEquals("Query 1", summaries.get(1).getTitle());
    }

   @Test
    void createConversationSummary_shouldTruncateLongTitles() {
        // --- Arrange ---
        String longQuery = "This is a very very long query that should definitely be over 50 characters to test the truncation logic";
        conversationService.storeMessage(userId, convoId, createRequest(longQuery), createResponse("OK"));

        // --- Act ---
        List<ConversationService.ConversationSummary> summaries = conversationService.getUserConversations(userId);

        // --- Assert ---
        assertEquals(1, summaries.size());

        // --- THIS IS THE FIX ---
        // The service logic is substring(0, 47) + "...", which results in "...defi..."
        // The old test hardcoded the wrong string ("...defin...").
        String expectedTitle = "This is a very very long query that should defi...";

        assertEquals(expectedTitle, summaries.get(0).getTitle());
        assertEquals(50, summaries.get(0).getTitle().length());
    }

    @Test
    void deleteConversation_shouldRemoveConversation() {
        // --- Arrange ---
        conversationService.storeMessage(userId, convoId, createRequest("To delete"), createResponse("..."));
        conversationService.storeMessage(userId, "convo-2", createRequest("To keep"), createResponse("..."));

        // --- Act ---
        boolean wasRemoved = conversationService.deleteConversation(userId, convoId);

        // --- Assert ---
        assertTrue(wasRemoved);
        assertNull(conversationService.getConversation(userId, convoId));
        
        List<ConversationService.ConversationSummary> summaries = conversationService.getUserConversations(userId);
        assertEquals(1, summaries.size());
        assertEquals("convo-2", summaries.get(0).getId());
    }

    @Test
    void deleteConversation_shouldReturnFalse_ifNotFound() {
        assertFalse(conversationService.deleteConversation(userId, "non-existent-convo"));
    }

    @Test
    void clearUserConversations_shouldRemoveAll() {
        // --- Arrange ---
        conversationService.storeMessage(userId, "convo-1", createRequest("1"), createResponse("1"));
        conversationService.storeMessage(userId, "convo-2", createRequest("2"), createResponse("2"));
        conversationService.storeMessage("other-user", "convo-3", createRequest("3"), createResponse("3"));

        // --- Act ---
        conversationService.clearUserConversations(userId);

        // --- Assert ---
        // User 1 is cleared
        assertTrue(conversationService.getUserConversations(userId).isEmpty());
        
        // Other user is unaffected
        assertFalse(conversationService.getUserConversations("other-user").isEmpty());
    }

    @Test
    void getStats_shouldReturnCorrectCounts() {
        // --- Arrange ---
        // User 1, Convo 1: 2 messages
        conversationService.storeMessage("user-1", "u1-c1", createRequest("1"), createResponse("1"));
        
        // User 2, Convo 1: 4 messages
        conversationService.storeMessage("user-2", "u2-c1", createRequest("2"), createResponse("2"));
        conversationService.storeMessage("user-2", "u2-c1", createRequest("3"), createResponse("3"));

        // User 2, Convo 2: 2 messages
        conversationService.storeMessage("user-2", "u2-c2", createRequest("4"), createResponse("4"));

        // --- Act ---
        ConversationService.ConversationStats stats = conversationService.getStats();

        // --- Assert ---
        assertEquals(2, stats.getTotalUsers());
        assertEquals(3, stats.getTotalConversations()); // u1-c1, u2-c1, u2-c2
        assertEquals(8, stats.getTotalMessages()); // 2 + 4 + 2
    }

    // Note: Testing the private limitUserConversations and the public Conversation.limitMessages
    // methods is best done by inference through the public 'storeMessage' method.
    // To properly test the eviction, you would need to use reflection to set the
    // MAX_... constants to small numbers, or loop 101 times.
    // For this example, we assume the logic shown in the public methods is correct.
}