package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgreementToolTest {

    @Mock
    private AgreementService agreementService;

    @InjectMocks
    private AgreementTool agreementTool;

    private ToolCall validToolCall;
    private Agreement agreementWithRvc;
    private Agreement agreementWithoutRvc;

    @BeforeEach
    void setUp() {
        // Create a valid tool call
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", "USA");
        validToolCall = new ToolCall("getTradeAgreements", args);

        // Create mock data
        agreementWithRvc = new Agreement();
        agreementWithRvc.setId(1L);
        agreementWithRvc.setName("USMCA");
        agreementWithRvc.setRvcThreshold(new BigDecimal("45.0"));

        agreementWithoutRvc = new Agreement();
        agreementWithoutRvc.setId(2L);
        agreementWithoutRvc.setName("US-Korea Free Trade Agreement");
        agreementWithoutRvc.setRvcThreshold(null);
    }

    @Test
    void testGetName() {
        assertEquals("getTradeAgreements", agreementTool.getName());
    }

    @Test
    void testGetDefinition() {
        // --- Act ---
        ToolDefinition def = agreementTool.getDefinition();

        // --- Assert ---
        assertEquals("getTradeAgreements", def.getName());
        assertTrue(def.getDescription().contains("Retrieve trade agreements"));
        
        Map<String, Object> params = def.getParameters();
        assertNotNull(params);
        
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertTrue(props.containsKey("countryIso3"));
        
        // Fix for ClassCastException: OpenAI spec requires a String array
        String[] requiredArray = (String[]) params.get("required");
        List<String> required = Arrays.asList(requiredArray);
        assertTrue(required.contains("countryIso3"));
    }

    @Test
    void execute_shouldReturnSuccess_onHappyPath() {
        // --- Arrange ---
        List<Agreement> mockAgreements = List.of(agreementWithRvc, agreementWithoutRvc);
        when(agreementService.getAgreementsByCountry("USA")).thenReturn(mockAgreements);

        // --- Act ---
        ToolResult result = agreementTool.execute(validToolCall);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertNull(result.getError());
        assertNotNull(result.getData());
        
        verify(agreementService).getAgreementsByCountry("USA");
        
        // Check formatting
        String data = result.getData();
        assertTrue(data.contains("Trade Agreements for Country: USA"));
        assertTrue(data.contains("Found 2 trade agreement(s)"));
        assertTrue(data.contains("1. USMCA"));
        assertTrue(data.contains("RVC Threshold: 45.0%"));
        assertTrue(data.contains("2. US-Korea Free Trade Agreement"));
        assertFalse(data.contains("RVC Threshold: null")); // Should not print null
        assertTrue(data.contains("Total Agreements: 2"));
        assertTrue(data.contains("Agreements with RVC data: 1"));
    }

    @Test
    void execute_shouldNormalizeParameters() {
        // --- Arrange ---
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", "  usa "); // Lowercase with spaces
        ToolCall call = new ToolCall("getTradeAgreements", args);
        
        when(agreementService.getAgreementsByCountry("USA")).thenReturn(List.of());

        // --- Act ---
        agreementTool.execute(call);

        // --- Assert ---
        // Verify the service was called with the *normalized* value
        verify(agreementService).getAgreementsByCountry("USA");
    }

    @Test
    void execute_shouldHandleEmptyAgreementList() {
        // --- Arrange ---
        when(agreementService.getAgreementsByCountry("USA")).thenReturn(List.of()); // Empty list

        // --- Act ---
        ToolResult result = agreementTool.execute(validToolCall);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().contains("No trade agreements found for USA."));
    }

    @Test
    void execute_shouldReturnError_whenCountryIsMissing() {
        // --- Arrange ---
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", null); // Missing value
        ToolCall call = new ToolCall("getTradeAgreements", args);

        // --- Act ---
        ToolResult result = agreementTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need to know which country you're asking about"));
        verify(agreementService, never()).getAgreementsByCountry(any());
    }

    @Test
    void execute_shouldReturnError_whenCountryIsEmpty() {
        // --- Arrange ---
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", "   "); // Empty value
        ToolCall call = new ToolCall("getTradeAgreements", args);

        // --- Act ---
        ToolResult result = agreementTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need to know which country you're asking about"));
        verify(agreementService, never()).getAgreementsByCountry(any());
    }

    @Test
    void execute_shouldReturnError_whenCountryFormatIsInvalid() {
        // --- Arrange ---
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", "US"); // Invalid 2-letter code
        ToolCall call = new ToolCall("getTradeAgreements", args);

        // --- Act ---
        ToolResult result = agreementTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The country code 'US' doesn't look right"));
        verify(agreementService, never()).getAgreementsByCountry(any());
    }

    @Test
    void execute_shouldReturnError_whenCountryFormatIsInvalidNonAlpha() {
        // --- Arrange ---
        Map<String, Object> args = new HashMap<>();
        args.put("countryIso3", "U$A"); // Invalid non-alpha
        ToolCall call = new ToolCall("getTradeAgreements", args);

        // --- Act ---
        ToolResult result = agreementTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The country code 'U$A' doesn't look right"));
        verify(agreementService, never()).getAgreementsByCountry(any());
    }

    @Test
    void execute_shouldHandleNotFoundException() {
        // --- Arrange ---
        when(agreementService.getAgreementsByCountry("USA"))
            .thenThrow(new RuntimeException("Country not found"));

        // --- Act ---
        ToolResult result = agreementTool.execute(validToolCall);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The country code you provided might not be in our database."));
    }
    
    @Test
    void execute_shouldHandleDatabaseException() {
        // --- Arrange ---
        when(agreementService.getAgreementsByCountry("USA"))
            .thenThrow(new RuntimeException("Error accessing database"));

        // --- Act ---
        ToolResult result = agreementTool.execute(validToolCall);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("problem connecting to the trade agreement database."));
    }
    
    @Test
    void execute_shouldHandleGenericException() {
        // --- Arrange ---
        when(agreementService.getAgreementsByCountry("USA"))
            .thenThrow(new NullPointerException("Unexpected error"));

        // --- Act ---
        ToolResult result = agreementTool.execute(validToolCall);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Verifying the country code is correct"));
    }
}