package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;

@Service
public class TariffRateServiceImpl implements TariffRateService {
    private final TariffRateRepository tariffRates;
    private final AgreementRepository agreements;
    private final CountryRepository countries;
    private final HsProductRepository hsProducts;

    public TariffRateServiceImpl(TariffRateRepository tariffRates, AgreementRepository agreements,
                                CountryRepository countries, HsProductRepository hsProducts) {
        this.tariffRates = tariffRates;
        this.agreements = agreements;
        this.countries = countries;
        this.hsProducts = hsProducts;
    }

    @Override
    public List<TariffRate> listTariffRates() {
        return tariffRates.findAll();
    }

    @Override
    public TariffRate getTariffRateById(Long id) {
        return tariffRates.findById(id)
            .orElseThrow(TariffRateNotFoundException::new);
    }

    // @Override
    // public TariffRate getTariffRateByImporterAndOriginAndHsProductIdAndBasis(
    //     Long importerId, Long originId, Long hsCode, String basis) {

    //     return tariffRates.findByImporterIdAndOriginIdAndHsProductIdAndBasis(importerId, originId, hsProductId, basis)
    //         .orElseThrow(TariffRateNotFoundException::new);
    // }

    @Override
    public TariffRateLookupDto getTariffRateWithAgreement(String importerIso2, String originIso2, String hsCode) {
        if (hsCode == null || hsCode.isBlank()) {
            throw new IllegalArgumentException("hsCode must be provided");
        }

        Country importer = countries.findByIso2IgnoreCase(importerIso2)
            .orElseThrow(() -> new IllegalArgumentException("Unknown importer ISO2: " + importerIso2));

        Country origin = null;
        if (originIso2 != null && !originIso2.isBlank()) {
            origin = countries.findByIso2IgnoreCase(originIso2)
                .orElseThrow(() -> new IllegalArgumentException("Unknown origin ISO2: " + originIso2));
        }

        HsProduct product = hsProducts
            .findByDestinationIdAndHsCode(importer.getId(), hsCode)
            .orElseThrow(() -> new TariffRateNotFoundException("No HS product found for importer " + importerIso2 + " and code " + hsCode));

        Long importerId = importer.getId();
        Long hsProductId = product.getId();

        // Determine MFN: prefer origin-specific MFN when origin provided; otherwise fall back to general MFN
        TariffRate tariffRateMfn = null;
        if (origin != null) {
            Long originId = origin.getId();
            tariffRateMfn = tariffRates
                .findByImporterIdAndOriginIdAndHsProductIdAndBasis(importerId, originId, hsProductId, "MFN")
                .orElse(null);
        }
        if (tariffRateMfn == null) {
            tariffRateMfn = tariffRates
                .findByImporterIdAndHsProductIdAndBasis(importerId, hsProductId, "MFN")
                .orElseThrow(() -> new TariffRateNotFoundException("No MFN tariff rate found for importer " + importerIso2 + " and HS code " + hsCode));
        }

        // Determine preferential rate: only when origin provided and there is a matching origin-specific PREF
        TariffRate tariffRatePref = null;
        if (origin != null) {
            Long originId = origin.getId();
            tariffRatePref = tariffRates
                .findByImporterIdAndOriginIdAndHsProductIdAndBasis(importerId, originId, hsProductId, "PREF")
                .orElse(null);
        }

        Agreement agreement = null;
        if (tariffRatePref != null && tariffRatePref.getAgreementId() != null) {
            agreement = agreements.findById(tariffRatePref.getAgreementId()).orElse(null);
        }

        return new TariffRateLookupDto(tariffRateMfn, tariffRatePref, agreement);
    }

    public BigDecimal calculateTariffRate(TariffRateRequestDto rq) {
        BigDecimal mfnRate = rq.getMfnRate();
        BigDecimal prefRate = rq.getPrefRate();
        BigDecimal rvcDefined = rq.getRvc(); 
        BigDecimal RVC = rq.getMaterialCost()
                        .add(rq.getLabourCost())
                        .add(rq.getOverheadCost())
                        .add(rq.getProfit())
                        .add(rq.getOtherCosts())
                        .divide(rq.getFob(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
    
        BigDecimal avRate = RVC.compareTo(rvcDefined) >= 0 ? prefRate : mfnRate; // apply pref if rvc >= defined
        BigDecimal totalValue = rq.getTotalValue();
        BigDecimal totalTariff = BigDecimal.ZERO;
        totalTariff = totalValue.multiply(avRate);
        return totalTariff;
    }
    
}
