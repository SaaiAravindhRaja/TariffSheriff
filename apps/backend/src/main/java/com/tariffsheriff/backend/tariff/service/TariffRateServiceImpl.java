package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    public List<TariffRate> listTariffRates(int limit) {
        // Use pagination to limit database query - MUCH faster!
        return tariffRates.findAll(PageRequest.of(0, limit)).getContent();
    }

    @Override
    public List<TariffRate> findByCountryPair(String importerIso3, String originIso3, String hsCodePrefix, int limit) {
        if (hsCodePrefix != null && !hsCodePrefix.isBlank()) {
            return tariffRates.findByCountryPairAndHsCode(importerIso3, originIso3, hsCodePrefix,
                    PageRequest.of(0, limit));
        }
        return tariffRates.findByCountryPair(importerIso3, originIso3, PageRequest.of(0, limit));
    }

    @Override
    public List<TariffRate> findByCountryPairAndHsCodes(String importerIso3, String originIso3, List<String> hsCodes,
            int limit) {
        // Pad list to 5 elements (use empty string for unused slots)
        String code1 = hsCodes.size() > 0 ? hsCodes.get(0) : "";
        String code2 = hsCodes.size() > 1 ? hsCodes.get(1) : "";
        String code3 = hsCodes.size() > 2 ? hsCodes.get(2) : "";
        String code4 = hsCodes.size() > 3 ? hsCodes.get(3) : "";
        String code5 = hsCodes.size() > 4 ? hsCodes.get(4) : "";
        return tariffRates.findByCountryPairAndHsCodes(importerIso3, originIso3, code1, code2, code3, code4, code5,
                PageRequest.of(0, limit));
    }

    @Override
    public List<TariffRate> findByImporter(String importerIso3, String hsCodePrefix, int limit) {
        if (hsCodePrefix != null && !hsCodePrefix.isBlank()) {
            return tariffRates.findByImporterAndHsCode(importerIso3, hsCodePrefix, PageRequest.of(0, limit));
        }
        return tariffRates.findByImporter(importerIso3, PageRequest.of(0, limit));
    }

    @Override
    public List<TariffRate> findByImporterAndHsCodes(String importerIso3, List<String> hsCodes, int limit) {
        String code1 = hsCodes.size() > 0 ? hsCodes.get(0) : "";
        String code2 = hsCodes.size() > 1 ? hsCodes.get(1) : "";
        String code3 = hsCodes.size() > 2 ? hsCodes.get(2) : "";
        String code4 = hsCodes.size() > 3 ? hsCodes.get(3) : "";
        String code5 = hsCodes.size() > 4 ? hsCodes.get(4) : "";
        return tariffRates.findByImporterAndHsCodes(importerIso3, code1, code2, code3, code4, code5,
                PageRequest.of(0, limit));
    }

    @Override
    public List<TariffRate> findByHsCodes(List<String> hsCodes, int limit) {
        String code1 = hsCodes.size() > 0 ? hsCodes.get(0) : "";
        String code2 = hsCodes.size() > 1 ? hsCodes.get(1) : "";
        String code3 = hsCodes.size() > 2 ? hsCodes.get(2) : "";
        String code4 = hsCodes.size() > 3 ? hsCodes.get(3) : "";
        String code5 = hsCodes.size() > 4 ? hsCodes.get(4) : "";
        return tariffRates.findByHsCodes(code1, code2, code3, code4, code5, PageRequest.of(0, limit));
    }

    @Override
    public List<TariffRate> findByHsCodePrefix(String hsCodePrefix, int limit) {
        return tariffRates.findByHsCodePrefix(hsCodePrefix, PageRequest.of(0, limit));
    }

    @Override
    public TariffRate getTariffRateById(Long id) {
        return tariffRates.findById(id)
                .orElseThrow(TariffRateNotFoundException::new);
    }

    // @Override
    // public TariffRate getTariffRateByImporterAndOriginAndHsProductIdAndBasis(
    // Long importerId, Long originId, Long hsCode, String basis) {

    // return
    // tariffRates.findByImporterIdAndOriginIdAndHsProductIdAndBasis(importerId,
    // originId, hsProductId, basis)
    // .orElseThrow(TariffRateNotFoundException::new);
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
                .orElseThrow(() -> new TariffRateNotFoundException(
                        "No HS product found for importer " + importerCode + " and code " + hsCode));

        Long hsProductId = product.getId();

        // Determine MFN: prefer origin-specific MFN when origin provided; otherwise
        // fall back to general MFN
        TariffRate tariffRateMfn = null;
        if (origin != null) {
            tariffRateMfn = tariffRates
                    .findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(importerCode, originCode, hsProductId, "MFN")
                    .orElse(null);
        }
        if (tariffRateMfn == null) {
            tariffRateMfn = tariffRates
                    .findByImporterIso3AndHsProductIdAndBasis(importerCode, hsProductId, "MFN")
                    .orElseThrow(() -> new TariffRateNotFoundException(
                            "No MFN tariff rate found for importer " + importerCode + " and HS code " + hsCode));
        }

        // Determine preferential rate: only when origin provided and there is a
        // matching origin-specific PREF
        TariffRate tariffRatePref = null;
        if (origin != null) {
            tariffRatePref = tariffRates
                    .findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(importerCode, originCode, hsProductId,
                            "PREF")
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
                options);
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
                threshold);
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
                rvcThreshold);
    }

    @Override
    public List<TariffRateLookupDto> getSubcategories(String importerIso3, String originIso3, String hsCodePrefix,
            int limit) {
        if (!StringUtils.hasText(importerIso3)) {
            throw new IllegalArgumentException("importerIso3 is required for HS subcategory lookups");
        }
        String importerCode = normalizeIso3(importerIso3);
        importerCode = countries.findByIso3IgnoreCase(importerCode)
                .map(Country::getIso3)
                .orElseThrow(() -> new IllegalArgumentException("Unknown importer ISO3: " + importerIso3));

        String originCode = null;
        if (StringUtils.hasText(originIso3)) {
            originCode = normalizeIso3(originIso3);
            originCode = countries.findByIso3IgnoreCase(originCode)
                    .map(Country::getIso3)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown origin ISO3: " + originIso3));
        }

        String prefix = sanitizeHsPrefix(hsCodePrefix);
        int cappedLimit = Math.max(1, Math.min(limit, 500));

        List<TariffRate> rates = fetchRatesForPrefix(importerCode, originCode, prefix, cappedLimit);
        if (rates.isEmpty() && originCode != null) {
            // Gracefully fall back to importer-only MFN data so the caller still gets detail
            rates = fetchRatesForPrefix(importerCode, null, prefix, cappedLimit);
        }

        final String responseImporterIso3 = importerCode;
        final String responseOriginIso3 = originCode;

        Map<Long, List<TariffRate>> grouped = rates.stream()
                .collect(LinkedHashMap::new,
                        (map, rate) -> map.computeIfAbsent(rate.getHsProductId(), key -> new ArrayList<>()).add(rate),
                        LinkedHashMap::putAll);

        Map<Long, Agreement> agreementCache = new java.util.HashMap<>();
        return grouped.values().stream()
                .map(list -> toLookupDto(list, responseImporterIso3, responseOriginIso3, agreementCache))
                .toList();
    }

    private List<TariffRate> fetchRatesForPrefix(String importerIso3, String originIso3, String prefix, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        if (originIso3 != null) {
            return tariffRates.findByCountryPairAndHsCode(importerIso3, originIso3, prefix, page);
        }
        if (importerIso3 != null) {
            return tariffRates.findByImporterAndHsCode(importerIso3, prefix, page);
        }
        return tariffRates.findByHsCodePrefix(prefix, page);
    }

    private TariffRateLookupDto toLookupDto(List<TariffRate> rates, String importerIso3, String originIso3,
            Map<Long, Agreement> agreementCache) {
        TariffRate head = rates.get(0);
        List<TariffRateOptionDto> options = rates.stream()
                .map(rate -> toOptionDto(rate, resolveAgreement(rate.getAgreementId(), agreementCache)))
                .toList();
        return new TariffRateLookupDto(importerIso3, originIso3, head.getHsCode(), options);
    }

    private Agreement resolveAgreement(Long id, Map<Long, Agreement> cache) {
        if (id == null) {
            return null;
        }
        return cache.computeIfAbsent(id, key -> agreements.findById(key).orElse(null));
    }

    private String normalizeIso3(String iso3) {
        return iso3 == null ? null : iso3.trim().toUpperCase();
    }

    private String sanitizeHsPrefix(String hsCodePrefix) {
        if (!StringUtils.hasText(hsCodePrefix)) {
            throw new IllegalArgumentException("hsCode prefix is required");
        }
        String digits = hsCodePrefix.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("HS code prefix must contain at least 4 digits");
        }
        return digits;
    }
}
