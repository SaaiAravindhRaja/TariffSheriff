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
    public TariffRateLookupDto getTariffRateWithAgreement(Long importerId, Long originId, Long hsCode) {
        TariffRate tariffRateMfn = tariffRates
            .findByImporterIdAndOriginIdAndHsCodeAndBasis(importerId, originId, hsCode, "MFN")
            .orElseGet(() -> {
                return tariffRates.findByImporterIdAndHsCodeAndBasis(importerId, hsCode, "MFN") // get mfn (exporter col will be null)
                    .orElseThrow(TariffRateNotFoundException::new);
            });

        TariffRate tariffRatePref = tariffRates
            .findByImporterIdAndOriginIdAndHsCodeAndBasis(importerId, originId, hsCode, "PREF")
            .orElseGet(() -> {
                return tariffRates.findByImporterIdAndHsCodeAndBasis(importerId, hsCode, "PREF") // get pref (some importer -> some exporter)
                    .orElseThrow(TariffRateNotFoundException::new);
            });

        Agreement agreement = tariffRatePref.getAgreementId() == null
            ? null
            : agreements.findById(tariffRatePref.getAgreementId()).orElse(null);

        return new TariffRateLookupDto(tariffRateMfn, tariffRatePref, agreement);
    }

    public BigDecimal calculateTariffRate(TariffRateRequestDto rq) {
         // can just stash both lookups in frontend from first basic info request and get from payload
        BigDecimal mfnRate = rq.getMfnRate();
        BigDecimal prefRate = rq.getPrefRate();
        // Agreement agreement = rq.agreement();
        // BigDecimal RVCdefined = BigDecimal.valueOf(agreement.getRvc());
        BigDecimal rvcDefined = rq.getRvc(); 
        BigDecimal RVC = rq.getMaterialCost()
                        .add(rq.getLabourCost())
                        .add(rq.getOverheadCost())
                        .add(rq.getProfit())
                        .add(rq.getOtherCosts())
                        .divide(rq.getFob(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)); // order of operations (a + b + c) / d
    
        BigDecimal avRate = RVC.compareTo(rvcDefined) >= 0 ? prefRate : mfnRate; // apply pref if rvc >= defined
        BigDecimal totalValue = rq.getTotalValue();
        BigDecimal totalTariff = BigDecimal.ZERO;
        totalTariff = totalValue.multiply(avRate);
        return totalTariff;
    }
    
}
