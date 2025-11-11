package com.tariffsheriff.backend.tariff.service;

import java.util.List;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.model.TariffRate;

public interface TariffRateService {
    List<TariffRate> listTariffRates(int limit);

    List<TariffRate> findByCountryPair(String importerIso3, String originIso3, String hsCodePrefix, int limit);

    List<TariffRate> findByCountryPairAndHsCodes(String importerIso3, String originIso3, List<String> hsCodes,
            int limit);

    List<TariffRate> findByImporter(String importerIso3, String hsCodePrefix, int limit);

    List<TariffRate> findByImporterAndHsCodes(String importerIso3, List<String> hsCodes, int limit);

    List<TariffRate> findByHsCodes(List<String> hsCodes, int limit);

    List<TariffRate> findByHsCodePrefix(String hsCodePrefix, int limit);

    TariffRate getTariffRateById(Long id);

    TariffRateLookupDto getTariffRateWithAgreement(String importerIso3, String originIso3, String hsCode);

    TariffCalculationResponse calculateTariffRate(TariffRateRequestDto tariffCalculationData);
}
