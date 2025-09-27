package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.model.TariffRate;

@Service
public interface TariffCalculationService {
    List<TariffRate> listTariffRates();

    TariffRate getTariffRateById(Long id);

    BigDecimal calculateTariffRate(TariffRateRequestDto tariffCalculationData);
}


