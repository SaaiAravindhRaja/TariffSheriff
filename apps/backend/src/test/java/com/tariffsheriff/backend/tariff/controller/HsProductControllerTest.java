package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsProductControllerTest {

    @Mock
    private HsProductService hsProductService;

    @Mock
    private HsProductRepository hsProductRepository;

    @InjectMocks
    private HsProductController controller;

    // --- Test Cases ---

    @Test
    void search_withEmptyQuery_returnsEmptyList() {
        // --- Act ---
        List<Map<String, Object>> result1 = controller.search("", 10);
        List<Map<String, Object>> result2 = controller.search("   ", 10);
        List<Map<String, Object>> result3 = controller.search(null, 10);

        // --- Assert ---
        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
        assertTrue(result3.isEmpty());
        
        // --- Verify ---
        verifyNoInteractions(hsProductService, hsProductRepository);
    }

    @Test
    void search_withNumericQuery_callsOnlyCodePrefix_whenLimitIsMet() {
        // --- Arrange ---
        String query = "0101";
        int limit = 2; // Capped at 2
        
        HsProduct prod1 = createProduct(1L, "010110", "Live Horses");
        HsProduct prod2 = createProduct(2L, "010120", "Live Asses");
        List<HsProduct> codeResults = List.of(prod1, prod2);

        when(hsProductRepository.findByHsCodePrefix(query, limit)).thenReturn(codeResults);

        // --- Act ---
        List<Map<String, Object>> result = controller.search(query, limit);

        // --- Assert ---
        assertEquals(2, result.size());
        assertEquals("010110", result.get(0).get("hsCode"));
        assertEquals("010120", result.get(1).get("hsCode"));

        // --- Verify ---
        verify(hsProductRepository).findByHsCodePrefix(query, limit);
        // Crucially, the description search is never called
        verify(hsProductService, never()).searchByDescription(any(), anyInt());
    }

    @Test
    void search_withNonNumericQuery_callsOnlyDescriptionSearch() {
        // --- Arrange ---
        String query = "Apples";
        int limit = 5; // Capped at 5

        HsProduct prod1 = createProduct(1L, "080810", "Apples, fresh");
        List<HsProduct> descResults = List.of(prod1);

        when(hsProductService.searchByDescription(query, limit)).thenReturn(descResults);

        // --- Act ---
        List<Map<String, Object>> result = controller.search(query, limit);

        // --- Assert ---
        assertEquals(1, result.size());
        assertEquals("080810", result.get(0).get("hsCode"));

        // --- Verify ---
        // Crucially, the code prefix search is never called
        verify(hsProductRepository, never()).findByHsCodePrefix(any(), anyInt());
        verify(hsProductService).searchByDescription(query, limit);
    }

    @Test
    void search_withNumericQuery_fillsWithDescriptionSearch_andDeDuplicates() {
        // --- Arrange ---
        String query = "01";
        int limit = 5; // Capped at 5

        // Product 1 & 2 come from the code search
        HsProduct prod1_code = createProduct(1L, "010100", "Live Horses");
        HsProduct prod2_common = createProduct(2L, "010200", "Live Bovine");
        List<HsProduct> codeResults = List.of(prod1_code, prod2_common);

        // Product 3 & 2 come from the description search (prod2 is a duplicate)
        HsProduct prod3_desc = createProduct(3L, "030300", "Fish, frozen (01)"); // Desc contains "01"
        List<HsProduct> descResults = List.of(prod3_desc, prod2_common); // prod2 is duplicated by ID

        when(hsProductRepository.findByHsCodePrefix(query, limit)).thenReturn(codeResults);
        when(hsProductService.searchByDescription(query, limit)).thenReturn(descResults);

        // --- Act ---
        List<Map<String, Object>> result = controller.search(query, limit);

        // --- Assert ---
        // We expect 3 unique results: prod1_code, prod2_common, and prod3_desc
        assertEquals(3, result.size());
        
        // Check that the order is preserved (code results first)
        assertEquals("010100", result.get(0).get("hsCode")); // prod1_code
        assertEquals("010200", result.get(1).get("hsCode")); // prod2_common
        assertEquals("030300", result.get(2).get("hsCode")); // prod3_desc (the de-duped item)

        // --- Verify ---
        verify(hsProductRepository).findByHsCodePrefix(query, limit);
        verify(hsProductService).searchByDescription(query, limit);
    }

    @Test
    void search_limitIsCapped_aboveMax() {
        // --- Arrange ---
        String query = "test";
        int overLimit = 100; // Will be capped at 25
        
        // Return an empty list just to complete the call
        when(hsProductService.searchByDescription(any(), anyInt())).thenReturn(List.of());

        // --- Act ---
        controller.search(query, overLimit);

        // --- Assert / Verify ---
        // Verify that the service was called with the *capped* limit of 25
        verify(hsProductService).searchByDescription(query, 25);
    }

    @Test
    void search_limitIsCapped_belowMin() {
        // --- Arrange ---
        String query = "test";
        int underLimit = 0; // Will be capped at 1
        
        when(hsProductService.searchByDescription(any(), anyInt())).thenReturn(List.of());

        // --- Act ---
        controller.search(query, underLimit);

        // --- Assert / Verify ---
        // Verify that the service was called with the *capped* limit of 1
        verify(hsProductService).searchByDescription(query, 1);
    }

  @Test
    void search_resultIsMappedCorrectly() {
        // --- Arrange ---
        String query = "0101";
        
        // Create a product with extra fields
        HsProduct product = new HsProduct();
        product.setId(1L);
        product.setHsCode("010110");
        product.setHsLabel("Live Horses");
        product.setHsVersion("2022"); // <-- FIXED: Use a real field
        product.setDestinationIso3("USA");  // <-- FIXED: Use a real field
        
        when(hsProductRepository.findByHsCodePrefix(any(), anyInt())).thenReturn(List.of(product));

        // --- Act ---
        List<Map<String, Object>> result = controller.search(query, 5);

        // --- Assert ---
        assertEquals(1, result.size());
        Map<String, Object> map = result.get(0);
        
        // This assertion is the point of the test:
        // Check that only the two specified fields are in the map
        assertEquals(2, map.size());
        assertTrue(map.containsKey("hsCode"));
        assertTrue(map.containsKey("hsLabel"));
        
        assertEquals("010110", map.get("hsCode"));
        assertEquals("Live Horses", map.get("hsLabel"));
    }

    // --- Helper Methods ---

    /**
     * Helper to create a minimal HsProduct for testing
     */
    private HsProduct createProduct(Long id, String hsCode, String hsLabel) {
        HsProduct product = new HsProduct();
        product.setId(id);
        product.setHsCode(hsCode);
        product.setHsLabel(hsLabel);
        return product;
    }
}