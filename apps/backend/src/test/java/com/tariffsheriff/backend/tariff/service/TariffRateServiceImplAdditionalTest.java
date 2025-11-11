package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.model.Country;
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
class TariffRateServiceImplAdditionalTest {

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
    void calculateTariffRate_appliesPref_whenRvcAboveThreshold() {
        TariffRateRequestDto rq = new TariffRateRequestDto();
        rq.setMfnRate(new BigDecimal("0.10"));
        rq.setPrefRate(new BigDecimal("0.05"));
        rq.setRvcThreshold(new BigDecimal("10"));
        rq.setMaterialCost(new BigDecimal("50"));
        rq.setLabourCost(new BigDecimal("10"));
        rq.setOverheadCost(new BigDecimal("5"));
        rq.setProfit(new BigDecimal("5"));
        rq.setOtherCosts(new BigDecimal("0"));
        rq.setFob(new BigDecimal("100"));
        rq.setTotalValue(new BigDecimal("200"));

        TariffCalculationResponse resp = svc.calculateTariffRate(rq);
        // rvc = (50+10+5+5+0)/100 *100 = 70%
    assertEquals(new BigDecimal("70.000000"), resp.rvc());
    assertEquals("PREF", resp.basis());
    assertEquals(new BigDecimal("0.05"), resp.appliedRate());
    assertEquals(new BigDecimal("10.00"), resp.totalDuty().setScale(2));
    }

    @Test
    void calculateTariffRate_usesMfn_whenPrefNotApplicable() {
        TariffRateRequestDto rq = new TariffRateRequestDto();
        rq.setMfnRate(new BigDecimal("0.10"));
        rq.setPrefRate(null);
        rq.setRvcThreshold(new BigDecimal("100"));
        rq.setMaterialCost(new BigDecimal("1"));
        rq.setLabourCost(new BigDecimal("1"));
        rq.setOverheadCost(new BigDecimal("1"));
        rq.setProfit(new BigDecimal("1"));
        rq.setOtherCosts(new BigDecimal("0"));
        rq.setFob(new BigDecimal("100"));
        rq.setTotalValue(new BigDecimal("50"));

        TariffCalculationResponse resp = svc.calculateTariffRate(rq);
    assertEquals("MFN", resp.basis());
    assertEquals(new BigDecimal("0.10"), resp.appliedRate());
    }

    @Test
    void getTariffRateWithAgreement_happyPath_includesPrefAndAgreement() {
        Country importer = new Country();
        importer.setIso3("GBR");
        when(countries.findByIso3IgnoreCase("GBR")).thenReturn(Optional.of(importer));

        Country origin = new Country();
        origin.setIso3("CHN");
        when(countries.findByIso3IgnoreCase("CHN")).thenReturn(Optional.of(origin));

        HsProduct product = new HsProduct();
        product.setId(77L);
        when(hsProducts.findByDestinationIso3IgnoreCaseAndHsCode("GBR", "0101")).thenReturn(Optional.of(product));

        TariffRate mfn = new TariffRate();
        mfn.setId(1L);
        mfn.setBasis("MFN");
        mfn.setAdValoremRate(new BigDecimal("0.10"));

        TariffRate pref = new TariffRate();
        pref.setId(2L);
        pref.setBasis("PREF");
        pref.setAdValoremRate(new BigDecimal("0.02"));
        pref.setAgreementId(5L);

    when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "MFN")).thenReturn(Optional.of(mfn));
        when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "PREF")).thenReturn(Optional.of(pref));

        Agreement ag = new Agreement();
        ag.setId(5L);
        ag.setName("FTA");
        ag.setRvcThreshold(new BigDecimal("10"));
        when(agreements.findById(5L)).thenReturn(Optional.of(ag));

        TariffRateLookupDto lookup = svc.getTariffRateWithAgreement("GBR", "CHN", "0101");
    assertEquals("GBR", lookup.importerIso3());
    assertEquals("CHN", lookup.originIso3());
    assertEquals("0101", lookup.hsCode());
    assertEquals(2, lookup.rates().size());
    assertEquals("PREF", lookup.rates().get(1).basis());
    assertEquals("FTA", lookup.rates().get(1).agreementName());
    }
}
