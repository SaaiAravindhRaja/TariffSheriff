package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;
// Import the repository
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository; 
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map; // Import for new test

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffRateControllerTest {

    @Mock
    TariffRateService service;

    @Mock // <-- THIS MOCK WAS MISSING
    TariffRateRepository tariffRateRepository;

    @InjectMocks
    TariffRateController controller;

    private TariffRateRequestDto validRequest;
    private TariffRate sampleRate;
    private TariffRateLookupDto sampleLookup;

    @BeforeEach
    void setUp() {
        // Setup sample TariffRate
        sampleRate = new TariffRate();
        sampleRate.setId(1L);
        sampleRate.setBasis("MFN");
        sampleRate.setAdValoremRate(new BigDecimal("0.10"));
        sampleRate.setImporterIso3("GBR");
        sampleRate.setHsProductId(123L);

        // Setup sample calculation request
        validRequest = new TariffRateRequestDto();
        validRequest.setMfnRate(new BigDecimal("0.10"));
        validRequest.setTotalValue(new BigDecimal("1000"));
        validRequest.setMaterialCost(new BigDecimal("500"));
        validRequest.setLabourCost(new BigDecimal("200"));
        validRequest.setOverheadCost(new BigDecimal("100"));
        validRequest.setProfit(new BigDecimal("100"));
        validRequest.setOtherCosts(new BigDecimal("100"));
        validRequest.setFob(new BigDecimal("1000"));

        // Setup sample lookup response
        TariffRateOptionDto option = new TariffRateOptionDto(1L, "MFN",
                new BigDecimal("0.10"), false, null, null, null, null);
        sampleLookup = new TariffRateLookupDto("GBR", "CHN", "0101", List.of(option));
    }

    // --- YOUR EXISTING TESTS (CORRECT) ---

    @Test
    void getTariffRates_delegatesToService_byCountryPairAndHsCodes() {
        // This is your original test, renamed for clarity
        // --- Arrange ---
        String importer = "GBR";
        String origin = "CHN";
        List<String> hsCodes = List.of("0101");
        int limit = 20;
        
        when(service.findByCountryPairAndHsCodes(importer, origin, hsCodes, limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(importer, origin, hsCodes, limit);
        
        // --- Assert ---
        assertEquals(1, result.size()); 
        assertEquals(1L, result.get(0).getId());
        
        // Verify the correct service method was called
        verify(service).findByCountryPairAndHsCodes(importer, origin, hsCodes, limit);
        // Verify no other branches were called
        verify(service, never()).listTariffRates(anyInt());
        verify(service, never()).findByCountryPair(any(), any(), any(), anyInt());
    }

    @Test
    void calculateTariffRate_delegatesAndReturns() {
        TariffCalculationResponse expectedResponse = new TariffCalculationResponse(
                "MFN",
                new BigDecimal("0.10"),
                new BigDecimal("100.00"),
                new BigDecimal("90.0"),
                null
        );
        when(service.calculateTariffRate(validRequest)).thenReturn(expectedResponse);

        var result = controller.calculateTariffRate(validRequest);
        
        assertEquals("MFN", result.basis());
        assertEquals(new BigDecimal("0.10"), result.appliedRate());
        assertEquals(new BigDecimal("100.00"), result.totalDuty());
        assertEquals(new BigDecimal("90.0"), result.rvc());
        verify(service).calculateTariffRate(validRequest);
    }

    @Test
    void lookup_delegatesToService() {
        when(service.getTariffRateWithAgreement("GBR", "CHN", "0101"))
                .thenReturn(sampleLookup);

        var result = controller.getTariffRateAndAgreement("GBR", "CHN", "0101");
        
        assertNotNull(result);
        assertEquals("GBR", result.importerIso3());
        assertEquals("CHN", result.originIso3());
        
        verify(service).getTariffRateWithAgreement("GBR", "CHN", "0101");
    }

    @Test
    void getTariffRate_delegatesToService() {
        when(service.getTariffRateById(1L)).thenReturn(sampleRate);

        var result = controller.getTariffRate(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(service).getTariffRateById(1L);
    }

    // --- NEW TESTS FOR MISSING LOGIC ---

    @Test
    void getTariffRates_delegatesToService_byCountryPair() {
        // --- Arrange ---
        String importer = "GBR";
        String origin = "CHN";
        int limit = 50;
        
        // Test the branch: importerIso3 != null && originIso3 != null
        when(service.findByCountryPair(importer, origin, null, limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(importer, origin, null, limit);
        
        // --- Assert ---
        assertEquals(1, result.size());
        verify(service).findByCountryPair(importer, origin, null, limit);
        verify(service, never()).listTariffRates(anyInt());
    }

    @Test
    void getTariffRates_delegatesToService_byImporterAndHsCodes() {
        // --- Arrange ---
        String importer = "GBR";
        List<String> hsCodes = List.of("0101");
        int limit = 50;
        
        // Test the branch: importerIso3 != null && hsCodes != null
        when(service.findByImporterAndHsCodes(importer, hsCodes, limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(importer, null, hsCodes, limit);
        
        // --- Assert ---
        assertEquals(1, result.size());
        verify(service).findByImporterAndHsCodes(importer, hsCodes, limit);
        verify(service, never()).listTariffRates(anyInt());
    }

    @Test
    void getTariffRates_delegatesToService_byImporter() {
        // --- Arrange ---
        String importer = "GBR";
        int limit = 50;
        
        // Test the branch: importerIso3 != null
        when(service.findByImporter(importer, null, limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(importer, null, null, limit);
        
        // --- Assert ---
        assertEquals(1, result.size());
        verify(service).findByImporter(importer, null, limit);
        verify(service, never()).listTariffRates(anyInt());
    }

    @Test
    void getTariffRates_delegatesToService_byHsCodes() {
        // --- Arrange ---
        List<String> hsCodes = List.of("0101");
        int limit = 50;
        
        // Test the branch: hsCodes != null
        when(service.findByHsCodes(hsCodes, limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(null, null, hsCodes, limit);
        
        // --- Assert ---
        assertEquals(1, result.size());
        verify(service).findByHsCodes(hsCodes, limit);
        verify(service, never()).listTariffRates(anyInt());
    }

    @Test
    void getTariffRates_delegatesToService_default() {
        // --- Arrange ---
        int limit = 1000; // The default value in the controller
        
        // Test the final else branch (no params)
        when(service.listTariffRates(limit))
                .thenReturn(List.of(sampleRate));

        // --- Act ---
        var result = controller.getTariffRates(null, null, null, limit);
        
        // --- Assert ---
        assertEquals(1, result.size());
        verify(service).listTariffRates(limit);
    }

    @Test
    void getTradeRoutes_delegatesToRepository_andMapsCorrectly() {
        // --- Arrange ---
        // This is the raw data that the repository returns
        Object[] route1 = new Object[]{"US", "CN", 150L};
        Object[] route2 = new Object[]{"GB", "FR", 75L};
        List<Object[]> mockRoutes = List.of(route1, route2);
        
        // Stub the repository call
        when(tariffRateRepository.findDistinctTradeRoutes()).thenReturn(mockRoutes);

        // --- Act ---
        List<Map<String, Object>> result = controller.getTradeRoutes();

        // --- Assert ---
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify the repository was called
        verify(tariffRateRepository).findDistinctTradeRoutes();

        // Verify the mapping logic in the controller
        Map<String, Object> map1 = result.get(0);
        assertEquals("US", map1.get("importerIso3"));
        assertEquals("CN", map1.get("originIso3"));
        assertEquals(150L, map1.get("count"));
        
        Map<String, Object> map2 = result.get(1);
        assertEquals("GB", map2.get("importerIso3"));
        assertEquals("FR", map2.get("originIso3"));
        assertEquals(75L, map2.get("count"));
    }
}