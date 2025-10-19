package com.tariffsheriff.backend.chatbot.integration;

import com.tariffsheriff.backend.chatbot.dto.ChatQueryRequest;
import com.tariffsheriff.backend.chatbot.dto.ChatQueryResponse;
import com.tariffsheriff.backend.chatbot.service.ChatbotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for chatbot functionality
 * Tests the complete flow: query -> tool selection -> tool execution -> response generation
 * 
 * Note: These tests require a valid OPENAI_API_KEY environment variable
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
public class ChatbotIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotIntegrationTest.class);
    
    @Autowired
    private ChatbotService chatbotService;
    
    /**
     * Test 8.1: Tariff lookup flow
     * Query: "What's the tariff rate for importing steel from China to USA?"
     * - Verify tool selection is correct
     * - Verify tool executes successfully
     * - Verify response is conversational
     */
    @Test
    public void testTariffLookupFlow() {
        logger.info("=== Test 8.1: Tariff Lookup Flow ===");
        
        // Arrange
        ChatQueryRequest request = new ChatQueryRequest();
        request.setQuery("What's the tariff rate for importing steel from China to USA?");
        request.setUserId("test-user-tariff");
        
        // Act
        long startTime = System.currentTimeMillis();
        ChatQueryResponse response = chatbotService.processQuery(request);
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isSuccess(), "Response should be successful");
        assertNotNull(response.getResponse(), "Response text should not be null");
        assertFalse(response.getResponse().isEmpty(), "Response text should not be empty");
        assertNotNull(response.getConversationId(), "Conversation ID should be set");
        assertNotNull(response.getToolsUsed(), "Tools used should be tracked");
        assertFalse(response.getToolsUsed().isEmpty(), "At least one tool should be used");
        
        // Verify tool selection
        String toolUsed = response.getToolsUsed().get(0);
        assertTrue(
            toolUsed.equals("tariff_lookup") || toolUsed.equals("hs_code_finder"),
            "Should use tariff_lookup or hs_code_finder tool, but used: " + toolUsed
        );
        
        // Verify response is conversational (contains common conversational elements)
        String responseText = response.getResponse().toLowerCase();
        boolean isConversational = 
            responseText.contains("tariff") || 
            responseText.contains("rate") || 
            responseText.contains("steel") ||
            responseText.contains("china") ||
            responseText.contains("usa") ||
            responseText.contains("import");
        
        assertTrue(isConversational, 
            "Response should be conversational and mention relevant terms. Response: " + response.getResponse());
        
        // Verify response time is reasonable (under 10 seconds)
        assertTrue(duration < 10000, 
            "Response should be generated in under 10 seconds, took: " + duration + "ms");
        
        logger.info("✓ Tariff lookup test passed");
        logger.info("  Tool used: {}", toolUsed);
        logger.info("  Response length: {} chars", response.getResponse().length());
        logger.info("  Processing time: {}ms", duration);
        logger.info("  Response preview: {}", 
            response.getResponse().substring(0, Math.min(150, response.getResponse().length())) + "...");
    }
    
    /**
     * Test 8.2: HS code search flow
     * Query: "Find HS code for coffee beans"
     * - Verify search returns relevant codes
     * - Verify response explains the codes
     */
    @Test
    public void testHsCodeSearchFlow() {
        logger.info("=== Test 8.2: HS Code Search Flow ===");
        
        // Arrange
        ChatQueryRequest request = new ChatQueryRequest();
        request.setQuery("Find HS code for coffee beans");
        request.setUserId("test-user-hscode");
        
        // Act
        long startTime = System.currentTimeMillis();
        ChatQueryResponse response = chatbotService.processQuery(request);
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isSuccess(), "Response should be successful");
        assertNotNull(response.getResponse(), "Response text should not be null");
        assertFalse(response.getResponse().isEmpty(), "Response text should not be empty");
        
        // Verify tool selection
        assertNotNull(response.getToolsUsed(), "Tools used should be tracked");
        assertFalse(response.getToolsUsed().isEmpty(), "At least one tool should be used");
        String toolUsed = response.getToolsUsed().get(0);
        assertEquals("hs_code_finder", toolUsed, 
            "Should use hs_code_finder tool for HS code search");
        
        // Verify response contains HS code information
        String responseText = response.getResponse();
        boolean containsHsCodeInfo = 
            responseText.matches(".*\\b\\d{4,8}\\b.*") || // Contains 4-8 digit code
            responseText.toLowerCase().contains("hs code") ||
            responseText.toLowerCase().contains("coffee");
        
        assertTrue(containsHsCodeInfo, 
            "Response should contain HS code information or explain coffee codes. Response: " + responseText);
        
        // Verify response time
        assertTrue(duration < 10000, 
            "Response should be generated in under 10 seconds, took: " + duration + "ms");
        
        logger.info("✓ HS code search test passed");
        logger.info("  Tool used: {}", toolUsed);
        logger.info("  Response length: {} chars", response.getResponse().length());
        logger.info("  Processing time: {}ms", duration);
        logger.info("  Response preview: {}", 
            response.getResponse().substring(0, Math.min(150, response.getResponse().length())) + "...");
    }
    
    /**
     * Test 8.3: Conversation context
     * Send multiple related queries in same conversation
     * - Verify context is maintained
     * - Verify follow-up questions work
     */
    @Test
    public void testConversationContext() {
        logger.info("=== Test 8.3: Conversation Context ===");
        
        // First query
        ChatQueryRequest request1 = new ChatQueryRequest();
        request1.setQuery("What countries does the US have trade agreements with?");
        request1.setUserId("test-user-context");
        
        ChatQueryResponse response1 = chatbotService.processQuery(request1);
        
        assertNotNull(response1, "First response should not be null");
        assertTrue(response1.isSuccess(), "First response should be successful");
        String conversationId = response1.getConversationId();
        assertNotNull(conversationId, "Conversation ID should be set");
        
        logger.info("  First query processed, conversation ID: {}", conversationId);
        
        // Follow-up query using same conversation
        ChatQueryRequest request2 = new ChatQueryRequest();
        request2.setQuery("What about Mexico specifically?");
        request2.setUserId("test-user-context");
        request2.setConversationId(conversationId);
        
        ChatQueryResponse response2 = chatbotService.processQuery(request2);
        
        assertNotNull(response2, "Second response should not be null");
        assertTrue(response2.isSuccess(), "Second response should be successful");
        assertEquals(conversationId, response2.getConversationId(), 
            "Conversation ID should be maintained");
        
        // Verify the follow-up response understands context
        String responseText = response2.getResponse().toLowerCase();
        boolean understandsContext = 
            responseText.contains("mexico") ||
            responseText.contains("usmca") ||
            responseText.contains("agreement") ||
            responseText.contains("trade");
        
        assertTrue(understandsContext, 
            "Follow-up response should understand context about Mexico. Response: " + response2.getResponse());
        
        logger.info("✓ Conversation context test passed");
        logger.info("  First response length: {} chars", response1.getResponse().length());
        logger.info("  Second response length: {} chars", response2.getResponse().length());
        logger.info("  Context maintained: true");
    }
    
    /**
     * Test 8.4: Error handling
     * - Test with invalid country codes
     * - Test with invalid HS codes
     * - Verify error messages are user-friendly
     */
    @Test
    public void testErrorHandling() {
        logger.info("=== Test 8.4: Error Handling ===");
        
        // Test with invalid country code
        ChatQueryRequest request1 = new ChatQueryRequest();
        request1.setQuery("What's the tariff rate from INVALIDCOUNTRY to USA?");
        request1.setUserId("test-user-error");
        
        ChatQueryResponse response1 = chatbotService.processQuery(request1);
        
        assertNotNull(response1, "Response should not be null even for invalid input");
        assertNotNull(response1.getResponse(), "Error response should have a message");
        
        // Verify error message is user-friendly (not technical stack trace)
        String errorResponse = response1.getResponse();
        assertFalse(errorResponse.contains("Exception"), 
            "Error message should not contain technical exception details");
        assertFalse(errorResponse.contains("Stack trace"), 
            "Error message should not contain stack trace");
        
        logger.info("  Invalid country test passed");
        logger.info("  Error response: {}", errorResponse.substring(0, Math.min(100, errorResponse.length())));
        
        // Test with invalid HS code format
        ChatQueryRequest request2 = new ChatQueryRequest();
        request2.setQuery("Find tariff for HS code INVALID123");
        request2.setUserId("test-user-error");
        
        ChatQueryResponse response2 = chatbotService.processQuery(request2);
        
        assertNotNull(response2, "Response should not be null for invalid HS code");
        assertNotNull(response2.getResponse(), "Error response should have a message");
        
        logger.info("  Invalid HS code test passed");
        
        // Test with empty query
        ChatQueryRequest request3 = new ChatQueryRequest();
        request3.setQuery("");
        request3.setUserId("test-user-error");
        
        ChatQueryResponse response3 = chatbotService.processQuery(request3);
        
        assertNotNull(response3, "Response should not be null for empty query");
        assertFalse(response3.isSuccess(), "Empty query should not be successful");
        
        logger.info("  Empty query test passed");
        logger.info("✓ Error handling tests passed");
    }
    
    /**
     * Test 8.5: Real user queries
     * Test with various real-world queries
     * - Verify accuracy of responses
     * - Verify conversational tone
     * - Verify response times are acceptable
     */
    @Test
    public void testRealUserQueries() {
        logger.info("=== Test 8.5: Real User Queries ===");
        
        String[] realQueries = {
            "Hello, can you help me with tariff information?",
            "What is USMCA?",
            "List all countries in the database",
            "Compare tariff rates for electronics from China and Mexico to USA",
            "What's the MFN rate for importing textiles?",
            "Find HS codes for automotive parts",
            "What trade agreements does Canada have?",
            "Explain the difference between MFN and preferential rates",
            "What's the tariff for importing wine from France to USA?",
            "How do I find the right HS code for my product?"
        };
        
        int successCount = 0;
        int totalTime = 0;
        
        for (int i = 0; i < realQueries.length; i++) {
            String query = realQueries[i];
            logger.info("  Testing query {}/{}: {}", i + 1, realQueries.length, query);
            
            ChatQueryRequest request = new ChatQueryRequest();
            request.setQuery(query);
            request.setUserId("test-user-real-" + i);
            
            long startTime = System.currentTimeMillis();
            ChatQueryResponse response = chatbotService.processQuery(request);
            long duration = System.currentTimeMillis() - startTime;
            totalTime += duration;
            
            // Verify response
            assertNotNull(response, "Response should not be null for query: " + query);
            assertNotNull(response.getResponse(), "Response text should not be null");
            assertFalse(response.getResponse().isEmpty(), "Response should not be empty");
            
            // Verify response time is acceptable (under 10 seconds)
            assertTrue(duration < 10000, 
                "Query should complete in under 10 seconds, took: " + duration + "ms for: " + query);
            
            // Verify conversational tone (response should be reasonably long and helpful)
            assertTrue(response.getResponse().length() > 20, 
                "Response should be substantial, got: " + response.getResponse().length() + " chars");
            
            if (response.isSuccess()) {
                successCount++;
            }
            
            logger.info("    ✓ Response: {} chars, {}ms, success: {}", 
                response.getResponse().length(), duration, response.isSuccess());
        }
        
        // Verify overall success rate
        double successRate = (double) successCount / realQueries.length;
        assertTrue(successRate >= 0.8, 
            "At least 80% of queries should succeed, got: " + (successRate * 100) + "%");
        
        // Verify average response time
        int avgTime = totalTime / realQueries.length;
        assertTrue(avgTime < 8000, 
            "Average response time should be under 8 seconds, got: " + avgTime + "ms");
        
        logger.info("✓ Real user queries test passed");
        logger.info("  Success rate: {}/{} ({}%)", successCount, realQueries.length, 
            (int)(successRate * 100));
        logger.info("  Average response time: {}ms", avgTime);
    }
}
