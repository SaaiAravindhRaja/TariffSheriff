package com.tariffsheriff.backend.chatbot.service.tools;

import com.tariffsheriff.backend.chatbot.dto.ToolCall;
import com.tariffsheriff.backend.chatbot.dto.ToolDefinition;
import com.tariffsheriff.backend.chatbot.dto.ToolResult;
import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsCodeFinderToolTest {

    @Mock
    private HsProductService hsProductService;

    @InjectMocks
    private HsCodeFinderTool hsCodeFinderTool;

    // Helper to create a ToolCall
    private ToolCall createToolCall(String productDescription) {
        Map<String, Object> args = new HashMap<>();
        args.put("productDescription", productDescription);
        return new ToolCall("findHsCode", args);
    }

    // Helper to create a mock HsProduct
    private HsProduct createMockProduct(String hsCode, String hsLabel) {
        HsProduct product = new HsProduct();
        product.setHsCode(hsCode);
        product.setHsLabel(hsLabel);
        return product;
    }

    @Test
    void testGetName() {
        assertEquals("findHsCode", hsCodeFinderTool.getName());
    }

    @Test
    void testGetDefinition() {
        // --- Act ---
        ToolDefinition def = hsCodeFinderTool.getDefinition();

        // --- Assert ---
        assertEquals("findHsCode", def.getName());
        assertTrue(def.getDescription().contains("Find HS codes"));
        
        Map<String, Object> params = def.getParameters();
        assertNotNull(params);
        
        Map<String, Object> props = (Map<String, Object>) params.get("properties");
        assertTrue(props.containsKey("productDescription"));
        
        // Correctly cast from String[] to List
        String[] requiredArray = (String[]) params.get("required");
        List<String> required = Arrays.asList(requiredArray);
        assertTrue(required.contains("productDescription"));
    }

    @Test
    void execute_shouldReturnSuccess_onHappyPath() {
        // --- Arrange ---
        ToolCall call = createToolCall("fresh avocados");
        HsProduct avocadoProduct = createMockProduct("080440", "Avocados, fresh or dried");
        
        when(hsProductService.searchByDescription("fresh avocados", 10))
            .thenReturn(List.of(avocadoProduct));

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertNull(result.getError());
        assertNotNull(result.getData());
        
        verify(hsProductService).searchByDescription("fresh avocados", 10);
        
        String data = result.getData();
        assertTrue(data.contains("Found 1 matching HS code(s)"));
        assertTrue(data.contains("1. HS Code: 080440"));
        assertTrue(data.contains("Description: Avocados, fresh or dried"));
        assertTrue(data.contains("Single match found."));
    }

    @Test
    void execute_shouldHandleEmptyResults() {
        // --- Arrange ---
        ToolCall call = createToolCall("unobtainium");
        when(hsProductService.searchByDescription("unobtainium", 10))
            .thenReturn(List.of()); // No results

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().contains("No matching HS codes found"));
        assertTrue(result.getData().contains("Suggestions:"));
    }

    @Test
    void execute_shouldHandleMultipleResults_andCountryInfo() {
        // --- Arrange ---
        ToolCall call = createToolCall("shoes");
        
        HsProduct product1 = createMockProduct("6403", "Footwear with outer soles of rubber... leather uppers");
        HsProduct product2 = createMockProduct("6404", "Footwear with outer soles of rubber... textile uppers");
        
        // Mock a product with destination info
        Country usa = new Country();
        usa.setName("United States");
        usa.setIso3("USA");
        product2.setDestination(usa);
        
        when(hsProductService.searchByDescription("shoes", 10))
            .thenReturn(List.of(product1, product2));

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertTrue(result.isSuccess());
        String data = result.getData();
        
        assertTrue(data.contains("Found 2 matching HS code(s)"));
        assertTrue(data.contains("1. HS Code: 6403"));
        assertTrue(data.contains("2. HS Code: 6404"));
        assertTrue(data.contains("Country: United States (USA)")); // Check for country info
        assertTrue(data.contains("Multiple matches found.")); // Check for multi-match guidance
    }

    @Test
    void execute_shouldReturnError_whenDescriptionIsNull() {
        // --- Arrange ---
        ToolCall call = createToolCall(null);

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need a product description"));
        verify(hsProductService, never()).searchByDescription(any(), anyInt());
    }

    @Test
    void execute_shouldReturnError_whenDescriptionIsEmpty() {
        // --- Arrange ---
        ToolCall call = createToolCall("   "); // Empty string

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("I need a product description"));
        verify(hsProductService, never()).searchByDescription(any(), anyInt());
    }
    
    @Test
    void execute_shouldReturnError_whenDescriptionIsTooShort() {
        // --- Arrange ---
        ToolCall call = createToolCall("a"); // Too short

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("product description is too short"));
        verify(hsProductService, never()).searchByDescription(any(), anyInt());
    }
    
    @Test
    void execute_shouldReturnError_whenDescriptionIsTooLong() {
        // --- Arrange ---
        String longString = "a".repeat(501);
        ToolCall call = createToolCall(longString); // Too long

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("product description is too long"));
        verify(hsProductService, never()).searchByDescription(any(), anyInt());
    }

    @Test
    void execute_shouldNormalizeDescription() {
        // --- Arrange ---
        ToolCall call = createToolCall("  leather shoes  ");
        when(hsProductService.searchByDescription("leather shoes", 10)).thenReturn(List.of());

        // --- Act ---
        hsCodeFinderTool.execute(call);

        // --- Assert ---
        // Verify the service was called with the *trimmed* description
        verify(hsProductService).searchByDescription("leather shoes", 10);
    }

    @Test
    void execute_shouldHandleDatabaseException() {
        // --- Arrange ---
        ToolCall call = createToolCall("coffee");
        when(hsProductService.searchByDescription("coffee", 10))
            .thenThrow(new RuntimeException("Error with database connection"));

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("problem connecting to the HS code database"));
    }
    
    @Test
    void execute_shouldHandleGenericException() {
        // --- Arrange ---
        ToolCall call = createToolCall("coffee");
        when(hsProductService.searchByDescription("coffee", 10))
            .thenThrow(new RuntimeException("Some other unexpected error"));

        // --- Act ---
        ToolResult result = hsCodeFinderTool.execute(call);

        // --- Assert ---
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Using simpler, more general terms"));
    }
}