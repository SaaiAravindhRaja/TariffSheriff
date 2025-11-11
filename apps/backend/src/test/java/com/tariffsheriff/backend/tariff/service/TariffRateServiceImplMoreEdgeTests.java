package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;
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
class TariffRateServiceImplMoreEdgeTests {

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
    void getTariffRateWithAgreement_prefExists_butAgreementMissing_usesRateAgreementId_andNoAgreementNameOrThreshold() {
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
        // rate has an agreement id, but repository will not return an Agreement
        pref.setAgreementId(5L);

        when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "MFN")).thenReturn(Optional.of(mfn));
        when(tariffRates.findByImporterIso3AndOriginIso3AndHsProductIdAndBasis("GBR", "CHN", 77L, "PREF")).thenReturn(Optional.of(pref));

        // agreements.findById returns empty -> agreement stays null
        when(agreements.findById(5L)).thenReturn(Optional.empty());

        TariffRateLookupDto lookup = svc.getTariffRateWithAgreement("GBR", "CHN", "0101");
        assertNotNull(lookup);
        assertEquals(2, lookup.rates().size());
        var option = lookup.rates().get(1);
        // since agreement was not found, agreementId should be taken from the rate
        assertEquals(5L, option.agreementId());
        assertNull(option.agreementName());
        assertNull(option.rvcThreshold());
    }

    @Test
    void getTariffRateWithAgreement_throwsWhenHsProductNotFound() {
        Country importer = new Country();
        importer.setIso3("GBR");
        when(countries.findByIso3IgnoreCase("GBR")).thenReturn(Optional.of(importer));

        // also ensure origin lookup does not fail before HS product lookup
        Country origin = new Country();
        origin.setIso3("CHN");
        when(countries.findByIso3IgnoreCase("CHN")).thenReturn(Optional.of(origin));

        when(hsProducts.findByDestinationIso3IgnoreCaseAndHsCode("GBR", "9999")).thenReturn(Optional.empty());

        assertThrows(TariffRateNotFoundException.class,
            () -> svc.getTariffRateWithAgreement("GBR", "CHN", "9999"));
    }
}
