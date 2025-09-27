package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.model.TariffRate;
import com.tariffsheriff.backend.model.enums.TariffBasis;
import com.tariffsheriff.backend.model.enums.TariffRateType;
import com.tariffsheriff.backend.repository.TariffRateRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;

@Service
public class TariffRateServiceImpl implements TariffCalculationService {
    private final TariffRateRepository tariffRates;

    public TariffRateServiceImpl(TariffRateRepository tariffRates){
        this.tariffRates = tariffRates;
    }

    public List<TariffRate> listTariffRates() {
        return tariffRates.findAll();
    }

    public TariffRate getTariffRateById(Long id) {
        return tariffRates.findById(id).orElseThrow(TariffRateNotFoundException::new);
    }

    private TariffRate resolveApplicableRate(Long importerId, Long originId, Long hsCode, String basis, LocalDate shipmentDate) {
        if (TariffBasis.MFN.getDbValue().equalsIgnoreCase(basis)) {
            return tariffRates.findApplicableMfn(importerId, hsCode, shipmentDate)
                .stream()
                .findFirst()
                .orElseThrow(TariffRateNotFoundException::new);
        } else if (TariffBasis.PREF.getDbValue().equalsIgnoreCase(basis)) {
            if (originId == null) {
                throw new TariffRateNotFoundException();
            }
            return tariffRates.findApplicablePreferential(importerId, originId, hsCode, shipmentDate)
                .stream()
                .findFirst()
                .orElseThrow(TariffRateNotFoundException::new);
        } else {
            throw new TariffRateNotFoundException();
        }
    }

    public BigDecimal calculateTariffRate(TariffRateRequestDto rq) {
        Long importerId = rq.getImporter_id() != null ? rq.getImporter_id().longValue() : null;
        Long originId = rq.getOrigin_id() != null ? rq.getOrigin_id().longValue() : null;
        Long hsCode = rq.getHsCode();
        LocalDate shipmentDate = LocalDate.now();

        TariffRate tariffRateMFN = resolveApplicableRate(importerId, null, hsCode, TariffBasis.MFN.getDbValue(), shipmentDate);
        TariffRate tariffRatePref = resolveApplicableRate(importerId, originId, hsCode, TariffBasis.PREF.getDbValue(), shipmentDate);

        java.math.BigDecimal rvcThresholdPercent = tariffRatePref.getAgreement() != null ? tariffRatePref.getAgreement().getRvcThreshold() : null;

        TariffRate appliedTariffRate;
        if (rvcThresholdPercent == null) {
            appliedTariffRate = tariffRateMFN; // fallback to MFN when threshold missing
        } else {
            BigDecimal rvcThresholdRatio = rvcThresholdPercent.movePointLeft(2); // convert percent (e.g., 40.00) to ratio (0.40)
            BigDecimal RVC = rq.getMaterialCost()
                            .add(rq.getLabourCost())
                            .add(rq.getOverheadCost())
                            .add(rq.getProfit())
                            .add(rq.getOtherCosts())
                            .divide(rq.getFOB(), 6, RoundingMode.HALF_UP);
            appliedTariffRate = RVC.compareTo(rvcThresholdRatio) >= 0 ? tariffRatePref : tariffRateMFN;
        }
        BigDecimal totalValue = rq.getTotalValue();
        BigDecimal totalTariff = BigDecimal.ZERO;

        TariffRateType type = appliedTariffRate.getRateType();
        if (type == TariffRateType.AD_VALOREM) {
            BigDecimal avRate = appliedTariffRate.getAdValoremRate();
            if (avRate != null) {
                totalTariff = totalValue.multiply(avRate);
            }
        } else if (type == TariffRateType.SPECIFIC) {
            BigDecimal specificAmount = appliedTariffRate.getSpecificAmount();
            if (specificAmount != null && rq.getQuantity() != null) {
                totalTariff = specificAmount.multiply(new BigDecimal(rq.getQuantity()));
            }
        } else if (type == TariffRateType.COMPOUND) {
            BigDecimal avRate = appliedTariffRate.getAdValoremRate();
            BigDecimal specificAmount = appliedTariffRate.getSpecificAmount();
            if (avRate != null) {
                totalTariff = totalValue.multiply(avRate);
            }
            if (specificAmount != null && rq.getQuantity() != null) {
                totalTariff = totalTariff.add(specificAmount.multiply(new BigDecimal(rq.getQuantity())));
            }
        }
        return totalTariff;
    }
}