package com.tariffsheriff.backend.chatbot.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffLookupToolTest {

    @Mock
    private TariffRateService tariffRateService;

    @Mock
    private ObjectMapper objectMapper; // Required by constructor, even if not used in execute

    @InjectMocks
    private TariffLookupTool tariffLookupTool;

    // Helper to create valid DTOs for mocking service responses
    private TariffRateLookupDto createMockLookupDto() {
        TariffRateOptionDto mfnRate = new TariffRateOptionDto(
            1L, "MFN", new BigDecimal("10.0"), false, null, null, null, null
        );
        TariffRateOptionDto prefRate = new TariffRateOptionDto(
            2L, "PREF", new BigDecimal("5.0"), false, null, 101L, "USMCA", new BigDecimal("40.0")
        );
        return new TariffRateLookupDto("USA", "CAN", "080440", List.of(mfnRate, prefRate));
    }
    
    // Helper to create a ToolCall
 private ToolCall createToolCall(String importer, String origin, String hsCode) {
    // Use a HashMap, which allows null values (unlike Map.of())
    Map<String, Object> args = new HashMap<>();
    args.put("importerIso3", importer);
    args.put("originIso3", origin);
    args.put("hsCode", hsCode);
    return new ToolCall("getTariffRate", args);
}

    @Test
    void testGetName() {
        assertEquals("getTariffRate", tariffLookupTool.getName());
    }

    @Test
    void testGetDefinition() {
        // --- Act ---
        ToolDefinition def = tariffLookupTool.getDefinition();

        // --- Assert ---
        assertEquals("getTariffRate", def.getName());
        assertTrue(def.getDescription().contains("Look up tariff rates"));
        
        Map<String, Object> params = def.getParameters();
        assertNotNull(params);
        
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertTrue(props.containsKey("importerIso3"));
        assertTrue(props.containsKey("originIso3"));
        assertTrue(props.containsKey("hsCode"));
        
      // Cast to the correct type (String array)
String[] requiredArray = (String[]) params.get("required");
// Convert to a List for easy assertions
List<String> required = Arrays.asList(requiredArray);

assertTrue(required.contains("importerIso3"));
assertTrue(required.contains("originIso3"));
assertTrue(required.contains("hsCode"));
    }

    @Test
    void execute_shouldReturnSuccess_onHappyPath() {
        // --- Arrange ---
        ToolCall call = createToolCall("usa", "chn", " 080440 "); // Test trimming
        TariffRateLookupDto mockDto = createMockLookupDto();
        
        // Stub the service call
        when(tariffRateService.getTariffRateWithAgreement("USA", "CHN", "080440"))
            .thenReturn(mockDto);

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertNull(result.getError());
        assertNotNull(result.getData());
        
        // Check that parameters were normalized
        verify(tariffRateService).getTariffRateWithAgreement("USA", "CHN", "080440");
        
        // Check that the formatting logic worked
        String data = result.getData();
        assertTrue(data.contains("Trade Route: CHN → USA"));
        assertTrue(data.contains("HS Code: 080440"));
        assertTrue(data.contains("Most Favored Nation (MFN) Rate:"));
        assertTrue(data.contains("- Rate: 10.0%"));
        assertTrue(data.contains("Preferential Rate(s):"));
        assertTrue(data.contains("- Rate: 5.0%"));
        assertTrue(data.contains("Trade Agreement: USMCA"));
        assertTrue(data.contains("RVC Threshold: 40.0%"));
    }

    @Test
    void execute_shouldReturnError_whenImporterIsMissing() {
        // --- Arrange ---
        ToolCall call = createToolCall(null, "CHN", "080440");

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need to know which country is importing"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }

    @Test
    void execute_shouldReturnError_whenOriginIsMissing() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", " ", "080440"); // Test empty string

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need to know which country the product is coming from"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }

    @Test
    void execute_shouldReturnError_whenHsCodeIsMissing() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", null);

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need the HS code for the product"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }

    @Test
    void execute_shouldReturnError_whenImporterFormatIsInvalid() {
        // --- Arrange ---
        ToolCall call = createToolCall("US", "CHN", "080440"); // Invalid 2-letter code

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The importing country code 'US' doesn't look right"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }

    @Test
    void execute_shouldReturnError_whenOriginFormatIsInvalid() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHINA", "080440"); // Invalid full name

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The origin country code 'CHINA' doesn't look right"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }
    
    @Test
    void execute_shouldReturnError_whenHsCodeFormatIsInvalid() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", "avocado"); // Invalid text

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("The HS code 'avocado' doesn't look valid"));
        verify(tariffRateService, never()).getTariffRateWithAgreement(any(), any(), any());
    }

    @Test
    void execute_shouldReturnFriendlyError_onNotFoundException() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", "080440");
        
        // Stub the service to throw a "not found" exception
        when(tariffRateService.getTariffRateWithAgreement("USA", "CHN", "080440"))
            .thenThrow(new RuntimeException("HS code not found for this route"));

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("This could mean:"));
        assertTrue(result.getError().contains("The HS code might not exist for this trade route"));
    }

    @Test
    void execute_shouldReturnGenericError_onOtherException() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", "080440");
        
        // Stub the service to throw a generic exception
        when(tariffRateService.getTariffRateWithAgreement("USA", "CHN", "080440"))
            .thenThrow(new RuntimeException("Database timeout"));

        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("There was a problem accessing the tariff database"));
    }

    @Test
    void formatTariffResult_shouldHandleNoRatesFound() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", "080440");
        // Return a DTO with an empty list
        TariffRateLookupDto mockDto = new TariffRateLookupDto("USA", "CHN", "080440", List.of());
        
        when(tariffRateService.getTariffRateWithAgreement("USA", "CHN", "080440"))
            .thenReturn(mockDto);
        
        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("No tariff rates found for this trade route and HS code."));
    }

    @Test
    void formatTariffResult_shouldHandleComplexRateOptions() {
        // --- Arrange ---
        ToolCall call = createToolCall("USA", "CHN", "080440");

        // 1. MFN Rate: 5% + $0.10/kg
        TariffRateOptionDto mfnRate = new TariffRateOptionDto(
            1L, "MFN", new BigDecimal("5.0"), true, "$0.10/kg", null, null, null
        );
        // 2. PREF Rate: Free (both ad valorem and non-ad valorem are null)
        TariffRateOptionDto prefRate = new TariffRateOptionDto(
            2L, "PREF", null, false, null, 101L, "USMCA", new BigDecimal("40.0")
        );
        TariffRateLookupDto mockDto = new TariffRateLookupDto("USA", "CHN", "080440", List.of(mfnRate, prefRate));

        when(tariffRateService.getTariffRateWithAgreement("USA", "CHN", "080440"))
            .thenReturn(mockDto);
        
        // --- Act ---
        ToolResult result = tariffLookupTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        String data = result.getData();
        assertTrue(data.contains("- Rate: 5.0% + $0.10/kg"));
        assertTrue(data.contains("- Rate: Free"));
        assertTrue(data.contains("Trade Agreement: USMCA"));
    }
}