package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffRateServiceImplEdgeCasesTest {

    @Mock
    TariffRateRepository tariffRates;

    @Mock
    AgreementRepository agreements;

    @Mock
    CountryRepository countries;

    @Mock
    HsProductRepository hsProducts;

    @InjectMocks
    TariffRateServiceImpl svc;

    @Test
    void getTariffRateWithAgreement_throwsWhenHsCodeBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> svc.getTariffRateWithAgreement("GBR", "CHN", "  "));
        assertTrue(ex.getMessage().contains("hsCode"));
    }

    @Test
    void getTariffRateWithAgreement_throwsOnUnknownImporter() {
        when(countries.findByIso3IgnoreCase("XXX")).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> svc.getTariffRateWithAgreement("XXX", "CHN", "0101"));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown importer") || ex.getMessage().contains("XXX"));
    }

    @Test
    void getTariffRateWithAgreement_originNull_returnsMfnOnly() {
        Country importer = new Country();
        importer.setIso3("GBR");
        when(countries.findByIso3IgnoreCase("GBR")).thenReturn(Optional.of(importer));

        HsProduct product = new HsProduct();
        product.setId(11L);
        when(hsProducts.findByDestinationIso3IgnoreCaseAndHsCode("GBR", "0101")).thenReturn(Optional.of(product));

        TariffRate mfn = new TariffRate();
        mfn.setId(9L);
        mfn.setBasis("MFN");
        mfn.setAdValoremRate(new BigDecimal("0.08"));

        when(tariffRates.findByImporterIso3AndHsProductIdAndBasis("GBR", 11L, "MFN")).thenReturn(Optional.of(mfn));

        var dto = svc.getTariffRateWithAgreement("GBR", null, "0101");
        assertEquals("GBR", dto.importerIso3());
        assertNull(dto.originIso3());
        assertEquals(1, dto.rates().size());
        assertEquals("MFN", dto.rates().get(0).basis());
    }

    @Test
    void getTariffRateWithAgreement_fallsBackToGeneralMfn_whenOriginSpecificMissing() {
        Country importer = new Country();
        importer.setIso3("GBR");
        when(countries.findByIso3IgnoreCase("GBR")).thenReturn(Optional.of(importer));

        Country origin = new Country();
        origin.setIso3("CHN");
        when(countries.findByIso3IgnoreCase("CHN")).thenReturn(Optional.of(origin));

        HsProduct product = new HsProduct();
        product.setId(77L);
        when(hsProducts.findByDestinationIso3IgnoreCaseAndHsCode("GBR", "0101")).thenReturn(Optional.of(product));

        TariffRate mfnGeneral = new TariffRate();
        mfnGeneral.setId(101L);
        mfnGeneral.setBasis("MFN");
        mfnGeneral.setAdValoremRate(new BigDecimal("0.12"));

        when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "MFN")).thenReturn(Optional.empty());
        when(tariffRates.findByImporterIso3AndHsProductIdAndBasis("GBR", 77L, "MFN")).thenReturn(Optional.of(mfnGeneral));
        when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "PREF")).thenReturn(Optional.empty());

        var dto = svc.getTariffRateWithAgreement("GBR", "CHN", "0101");
        assertEquals(1, dto.rates().size());
        assertEquals("MFN", dto.rates().get(0).basis());
        assertEquals(101L, dto.rates().get(0).id());
    }

    @Test
    void calculateTariffRate_appliesPref_whenRvcEqualsThreshold() {
        TariffRateRequestDto rq = new TariffRateRequestDto();
        rq.setMfnRate(new BigDecimal("0.10"));
        rq.setPrefRate(new BigDecimal("0.05"));
        rq.setRvcThreshold(new BigDecimal("10"));
        // costs sum to 10% of fob so rvc == threshold
        rq.setMaterialCost(new BigDecimal("5"));
        rq.setLabourCost(new BigDecimal("2"));
        rq.setOverheadCost(new BigDecimal("1"));
        rq.setProfit(new BigDecimal("1"));
        rq.setOtherCosts(new BigDecimal("1"));
        rq.setFob(new BigDecimal("100"));
        rq.setTotalValue(new BigDecimal("1000"));

        TariffCalculationResponse resp = svc.calculateTariffRate(rq);
        assertEquals("PREF", resp.basis());
        assertEquals(new BigDecimal("0.05"), resp.appliedRate());
    }
}
