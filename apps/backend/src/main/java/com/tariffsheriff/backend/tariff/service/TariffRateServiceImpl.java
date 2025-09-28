package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;

@Service
public class TariffRateServiceImpl implements TariffRateService {
    private final TariffRateRepository tariffRates;
    private final AgreementRepository agreements;

    public TariffRateServiceImpl(TariffRateRepository tariffRates, AgreementRepository agreements) {
        this.tariffRates = tariffRates;
        this.agreements = agreements;
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

    @Override
    public TariffRate getTariffRateByImporterAndOriginAndHsCodeAndBasis(
        Long importerId, Long originId, Long hsCode, String basis) {

        return tariffRates.findByImporterIdAndOriginIdAndHsCodeAndBasis(importerId, originId, hsCode, basis)
            .orElseThrow(TariffRateNotFoundException::new);
    }

    @Override
    public TariffRateLookupDto getTariffRateWithAgreement(Long importerId, Long originId, Long hsCode, String basis) {
        TariffRate rate = tariffRates
            .findByImporterIdAndOriginIdAndHsCodeAndBasis(importerId, originId, hsCode, basis)
            .orElseGet(() -> {
                return tariffRates.findByImporterIdAndHsCodeAndBasis(importerId, hsCode, basis)
                    .orElseThrow(TariffRateNotFoundException::new);
            });

        Agreement agreement = rate.getAgreementId() == null
            ? null
            : agreements.findById(rate.getAgreementId()).orElse(null);

        return new TariffRateLookupDto(rate, agreement);
    }

    public BigDecimal calculateTariffRate(TariffRateRequestDto rq) {
        TariffRateLookupDto mfnLookup = getTariffRateWithAgreement(rq.getImporter_id(), rq.getOrigin_id(), rq.getHsCode(), "MFN");
        TariffRateLookupDto prefLookup = getTariffRateWithAgreement(rq.getImporter_id(), rq.getOrigin_id(), rq.getHsCode(), "PREF");
        Agreement agreement = prefLookup.agreement();
        BigDecimal RVCdefined = BigDecimal.valueOf(agreement.getRvc());
        BigDecimal RVC = rq.getMaterialCost()
                        .add(rq.getLabourCost())
                        .add(rq.getOverheadCost())
                        .add(rq.getProfit())
                        .add(rq.getOtherCosts())
                        .divide(rq.getFob(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)); // order of operations (a + b + c) / d
    
        TariffRate appliedTariffRate = RVC.compareTo(RVCdefined) >= 0 ? prefLookup.tariffRate() : mfnLookup.tariffRate(); // apply pref if rvc >= defined
        BigDecimal totalValue = rq.getTotalValue();
        BigDecimal totalTariff = BigDecimal.ZERO;
        BigDecimal avRate = appliedTariffRate.getAdValoremRate();
        totalTariff = totalValue.multiply(avRate);
        return totalTariff;
    }
    
}
