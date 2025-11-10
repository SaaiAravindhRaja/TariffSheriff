package com.tariffsheriff.backend.tariffcalculation.service;

import java.util.Optional;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TariffCalculationService {
    TariffCalculation saveForUser(User user,
                                  TariffRateRequestDto input,
                                  com.tariffsheriff.backend.tariffcalculation.dto.FrontendCalculationResult result,
                                  String name,
                                  String notes,
                                  String hsCode,
                                  String importerIso2,
                                  String originIso2);

    Page<TariffCalculation> listForUser(Long userId, Pageable pageable);

    Optional<TariffCalculation> getForUser(Long id, Long userId);

    void deleteForUser(Long id, Long userId);
}
