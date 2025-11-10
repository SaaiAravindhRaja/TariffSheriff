package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffRateControllerTest {

    @Mock
    TariffRateService service;

    @InjectMocks
    TariffRateController controller;

    @Test
    void getTariffRates_delegatesToService() {
        TariffRate r = new TariffRate();
        r.setId(1L);
        when(service.listTariffRates()).thenReturn(List.of(r));

        var out = controller.getTariffRates();
        assertEquals(1, out.size());
        assertEquals(1L, out.get(0).getId());
    }

    @Test
    void calculateTariffRate_delegatesAndReturns() {
        var req = new com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto();
        req.setMfnRate(new BigDecimal("0.1"));
        TariffCalculationResponse resp = new TariffCalculationResponse("MFN", new BigDecimal("0.1"), new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("0"));
        when(service.calculateTariffRate(req)).thenReturn(resp);

        var out = controller.calculateTariffRate(req);
        assertEquals("MFN", out.basis());
        assertEquals(new BigDecimal("10"), out.totalDuty());
    }

    @Test
    void lookup_delegatesToService() {
        TariffRateLookupDto dto = new TariffRateLookupDto("GBR", "CHN", "0101", List.of());
        when(service.getTariffRateWithAgreement("GBR", "CHN", "0101")).thenReturn(dto);

        var out = controller.getTariffRateAndAgreement("GBR", "CHN", "0101");
        assertEquals("GBR", out.importerIso3());
    }
}
