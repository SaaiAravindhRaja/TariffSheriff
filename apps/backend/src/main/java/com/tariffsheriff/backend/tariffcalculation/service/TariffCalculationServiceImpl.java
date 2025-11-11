package com.tariffsheriff.backend.tariffcalculation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import com.tariffsheriff.backend.tariffcalculation.repository.TariffCalculationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TariffCalculationServiceImpl implements TariffCalculationService {

    private final TariffCalculationRepository repo;

    public TariffCalculationServiceImpl(TariffCalculationRepository repo) {
        this.repo = repo;
    }

    @Override
    public TariffCalculation saveForUser(User user,
                                         TariffRateRequestDto input,
                                         com.tariffsheriff.backend.tariffcalculation.dto.FrontendCalculationResult result,
                                         String name,
                                         String notes,
                                         String hsCode,
                                         String importerIso3,
                                         String originIso3) {
        // Trust frontend-provided results
        BigDecimal rvcComputed = result != null ? result.getCalculatedRvc() : null;
        String rateUsed = result != null ? result.getTariffBasis() : null;
        BigDecimal appliedRate = result != null ? result.getAppliedRate() : null;
        BigDecimal totalTariff = result != null ? result.getTotalCost() : null;

        TariffCalculation tc = new TariffCalculation();
        tc.setUser(user);
        tc.setName(name);
        tc.setNotes(notes);
        tc.setHsCode(hsCode);
        tc.setImporterIso3(importerIso3);
        tc.setOriginIso3(originIso3);

        // inputs
        tc.setMfnRate(input.getMfnRate());
        tc.setPrefRate(input.getPrefRate());
        // rvc threshold: prefer FE-provided threshold, fallback to request input
        if (result != null && result.getRvcThreshold() != null) {
            tc.setRvc(result.getRvcThreshold());
        } else {
            tc.setRvc(input.getRvcThreshold());
        }
        tc.setAgreementId(input.getAgreementId());
        tc.setQuantity(input.getQuantity());
        tc.setTotalValue(input.getTotalValue());
        tc.setMaterialCost(input.getMaterialCost());
        tc.setLabourCost(input.getLabourCost());
        tc.setOverheadCost(input.getOverheadCost());
        tc.setProfit(input.getProfit());
        tc.setOtherCosts(input.getOtherCosts());
        tc.setFob(input.getFob());
        tc.setNonOriginValue(input.getNonOriginValue());

        // computed
        tc.setRvcComputed(rvcComputed);
        tc.setRateUsed(rateUsed);
        tc.setAppliedRate(appliedRate);
        tc.setTotalTariff(totalTariff);

        return repo.save(tc);
    }

    @Override
    public Page<TariffCalculation> listForUser(Long userId, Pageable pageable) {
        return repo.findByUser_Id(userId, pageable);
    }

    @Override
    public Optional<TariffCalculation> getForUser(Long id, Long userId) {
        return repo.findByIdAndUser_Id(id, userId);
    }

    @Override
    public boolean deleteForUser(Long id, Long userId) {
        long deleted = repo.deleteByIdAndUser_Id(id, userId);
        return deleted > 0;
    }
}
