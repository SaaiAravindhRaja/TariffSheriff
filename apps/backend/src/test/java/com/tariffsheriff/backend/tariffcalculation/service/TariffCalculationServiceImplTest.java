package com.tariffsheriff.backend.tariffcalculation.service;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.dto.FrontendCalculationResult;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import com.tariffsheriff.backend.tariffcalculation.repository.TariffCalculationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffCalculationServiceImplTest {

    @Mock
    TariffCalculationRepository repo;

    @InjectMocks
    TariffCalculationServiceImpl svc;

    private TariffRateRequestDto sampleInput() {
        TariffRateRequestDto dto = new TariffRateRequestDto();
        dto.setMfnRate(new BigDecimal("0.05"));
        dto.setPrefRate(new BigDecimal("0.03"));
        dto.setRvcThreshold(new BigDecimal("20"));
        dto.setAgreementId(7L);
        dto.setQuantity(10);
        dto.setTotalValue(new BigDecimal("1000.00"));
        dto.setMaterialCost(new BigDecimal("200"));
        dto.setLabourCost(new BigDecimal("100"));
        dto.setOverheadCost(new BigDecimal("50"));
        dto.setProfit(new BigDecimal("25"));
        dto.setOtherCosts(new BigDecimal("5"));
        dto.setFob(new BigDecimal("100"));
        dto.setNonOriginValue(new BigDecimal("10"));
        return dto;
    }

    @Test
    void saveForUser_prefersFrontendRvcThreshold_whenProvided() {
        User user = new User();
        user.setId(42L);

        TariffRateRequestDto input = sampleInput();

        FrontendCalculationResult result = new FrontendCalculationResult();
        result.setRvcThreshold(new BigDecimal("30")); // should override input.rvcThreshold
        result.setCalculatedRvc(new BigDecimal("29.5"));
        result.setTariffBasis("MFN");
        result.setAppliedRate(new BigDecimal("0.05"));
        result.setTotalCost(new BigDecimal("123.45"));

        ArgumentCaptor<TariffCalculation> cap = ArgumentCaptor.forClass(TariffCalculation.class);
        when(repo.save(cap.capture())).thenAnswer(inv -> inv.getArgument(0));

        TariffCalculation saved = svc.saveForUser(user, input, result, "n", "notes", "HS123", "GBR", "CHN");

        // verify repo.save was called and fields mapped
        verify(repo).save(saved);

        TariffCalculation tc = cap.getValue();
        assertEquals(user, tc.getUser());
        assertEquals("n", tc.getName());
        assertEquals("notes", tc.getNotes());
        assertEquals("HS123", tc.getHsCode());
        assertEquals("GBR", tc.getImporterIso3());
        assertEquals("CHN", tc.getOriginIso3());

        // inputs mapped
        assertEquals(new BigDecimal("0.05"), tc.getMfnRate());
        assertEquals(new BigDecimal("0.03"), tc.getPrefRate());
        // rvc should prefer frontend result.rvcThreshold
        assertEquals(new BigDecimal("30"), tc.getRvc());
        assertEquals(7L, tc.getAgreementId());

        // computed fields from FE
        assertEquals(new BigDecimal("29.5"), tc.getRvcComputed());
        assertEquals("MFN", tc.getRateUsed());
        assertEquals(new BigDecimal("0.05"), tc.getAppliedRate());
        assertEquals(new BigDecimal("123.45"), tc.getTotalTariff());
    }

    @Test
    void saveForUser_fallsBackToInputRvc_whenFrontendDoesNotProvide() {
        User user = new User();
        TariffRateRequestDto input = sampleInput();

        // null result -> use input.rvcThreshold
        ArgumentCaptor<TariffCalculation> cap = ArgumentCaptor.forClass(TariffCalculation.class);
        when(repo.save(cap.capture())).thenAnswer(inv -> inv.getArgument(0));

    TariffCalculation saved = svc.saveForUser(user, input, null, "nm", null, null, null, null);
    assertNotNull(saved);

    TariffCalculation tc = cap.getValue();
        assertEquals(new BigDecimal("20"), tc.getRvc());
    }
}
