package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;

@Service
public interface TariffRateService {
    List<TariffRate> listTariffRates();

    TariffRate getTariffRateById(Long id);

    TariffRate getTariffRateByImporterAndOriginAndHsCodeAndBasis(Long importerId, Long originId, Long hsCode, String basis);

    TariffRateLookupDto getTariffRateWithAgreement(Long importerId, Long originId, Long hsCode);

    BigDecimal calculateTariffRate(TariffRateRequestDto tariffCalculationData);
}
