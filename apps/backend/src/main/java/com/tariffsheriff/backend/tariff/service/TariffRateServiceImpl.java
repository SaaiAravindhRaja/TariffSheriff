package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateOptionDto;
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
    public TariffRateLookupDto getTariffRateWithAgreement(String importerIso3, String originIso3, String hsCode) {
        if (hsCode == null || hsCode.isBlank()) {
            throw new IllegalArgumentException("hsCode must be provided");
        }

        Country importer = countries.findByIso3IgnoreCase(importerIso3)
            .orElseThrow(() -> new IllegalArgumentException("Unknown importer ISO3: " + importerIso3));

        Country origin = null;
        if (originIso3 != null && !originIso3.isBlank()) {
            origin = countries.findByIso3IgnoreCase(originIso3)
                .orElseThrow(() -> new IllegalArgumentException("Unknown origin ISO3: " + originIso3));
        }

        String importerCode = importer.getIso3();
        String originCode = origin != null ? origin.getIso3() : null;

        HsProduct product = hsProducts
            .findByDestinationIso3IgnoreCaseAndHsCode(importerCode, hsCode)
            .orElseThrow(() -> new TariffRateNotFoundException("No HS product found for importer " + importerCode + " and code " + hsCode));

        Long hsProductId = product.getId();

        // Determine MFN: prefer origin-specific MFN when origin provided; otherwise fall back to general MFN
        TariffRate tariffRateMfn = null;
        if (origin != null) {
            tariffRateMfn = tariffRates
                .findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(importerCode, originCode, hsProductId, "MFN")
                .orElse(null);
        }
        if (tariffRateMfn == null) {
            tariffRateMfn = tariffRates
                .findByImporterIso3AndHsProductIdAndBasis(importerCode, hsProductId, "MFN")
                .orElseThrow(() -> new TariffRateNotFoundException("No MFN tariff rate found for importer " + importerCode + " and HS code " + hsCode));
        }

        // Determine preferential rate: only when origin provided and there is a matching origin-specific PREF
        TariffRate tariffRatePref = null;
        if (origin != null) {
            tariffRatePref = tariffRates
                .findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(importerCode, originCode, hsProductId, "PREF")
                .orElse(null);
        }

        Agreement agreement = null;
        if (tariffRatePref != null && tariffRatePref.getAgreementId() != null) {
            agreement = agreements.findById(tariffRatePref.getAgreementId()).orElse(null);
        }

        List<TariffRateOptionDto> options = new ArrayList<>();
        options.add(toOptionDto(tariffRateMfn, null));

        if (tariffRatePref != null) {
            options.add(toOptionDto(tariffRatePref, agreement));
        }

        return new TariffRateLookupDto(
            importerCode,
            originCode,
            hsCode,
            options
        );
    }


    public com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse calculateTariffRate(TariffRateRequestDto rq) {
        BigDecimal mfnRate = rq.getMfnRate();
        BigDecimal prefRate = rq.getPrefRate();
        BigDecimal threshold = rq.getRvcThreshold();

        BigDecimal rvc = rq.getMaterialCost()
                .add(rq.getLabourCost())
                .add(rq.getOverheadCost())
                .add(rq.getProfit())
                .add(rq.getOtherCosts())
                .divide(rq.getFob(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        boolean canApplyPref = prefRate != null && threshold != null && rvc.compareTo(threshold) >= 0;
        BigDecimal appliedRate = (canApplyPref ? prefRate : mfnRate);
        String basis = (canApplyPref ? "PREF" : "MFN");

        BigDecimal totalDuty = rq.getTotalValue().multiply(appliedRate);

        return new com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse(
                basis,
                appliedRate,
                totalDuty,
                rvc,
                threshold
        );
    }

    private TariffRateOptionDto toOptionDto(TariffRate rate, Agreement agreement) {
        BigDecimal rvcThreshold = agreement != null ? agreement.getRvcThreshold() : null;
        String agreementName = agreement != null ? agreement.getName() : null;
        Long agreementId = agreement != null ? agreement.getId() : rate.getAgreementId();

        return new TariffRateOptionDto(
            rate.getId(),
            rate.getBasis(),
            rate.getAdValoremRate(),
            rate.isNonAdValorem(),
            rate.getNonAdValoremText(),
            agreementId,
            agreementName,
            rvcThreshold
        );
    }
    
}
