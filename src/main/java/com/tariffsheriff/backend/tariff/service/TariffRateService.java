package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;

@Service
public interface TariffRateService {
    List<TariffRate> listTariffRates();

    TariffRate getTariffRateById(Long id);

    TariffRate getTariffRateByImporterAndOriginAndHsCodeAndBasis(Long importer_id, Long origin_id, Long hsCode, String basis);

    BigDecimal calculateTariffRate(TariffRateRequestDto tariffCalculationData);

}