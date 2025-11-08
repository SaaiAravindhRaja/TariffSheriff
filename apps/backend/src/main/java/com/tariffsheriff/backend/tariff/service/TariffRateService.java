package com.tariffsheriff.backend.tariff.service;

import java.util.List;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.model.TariffRate;

public interface TariffRateService {
    List<TariffRate> listTariffRates();

    TariffRate getTariffRateById(Long id);

    TariffRateLookupDto getTariffRateWithAgreement(String importerIso3, String originIso3, String hsCode);

    TariffCalculationResponse calculateTariffRate(TariffRateRequestDto tariffCalculationData);
}
