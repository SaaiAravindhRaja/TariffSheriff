package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffRateControllerTest {

    @Mock
    TariffRateService service;

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

    @Test
    void getTariffRates_delegatesToService() {
        when(service.listTariffRates()).thenReturn(List.of(sampleRate));

        var result = controller.getTariffRates();
        
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("MFN", result.get(0).getBasis());
        assertEquals(new BigDecimal("0.10"), result.get(0).getAdValoremRate());
        verify(service).listTariffRates();
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
        assertEquals("0101", result.hsCode());
        assertEquals(1, result.rates().size());
        
        var rate = result.rates().get(0);
        assertEquals("MFN", rate.basis());
        assertEquals(new BigDecimal("0.10"), rate.adValoremRate());
        assertFalse(rate.nonAdValorem());
        
        verify(service).getTariffRateWithAgreement("GBR", "CHN", "0101");
    }

    @Test
    void getTariffRate_delegatesToService() {
        when(service.getTariffRateById(1L)).thenReturn(sampleRate);

        var result = controller.getTariffRate(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("MFN", result.getBasis());
        assertEquals(new BigDecimal("0.10"), result.getAdValoremRate());
        verify(service).getTariffRateById(1L);
    }
}
