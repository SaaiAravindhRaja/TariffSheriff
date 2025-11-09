package com.tariffsheriff.backend.tariff.dto;

import java.util.List;

/**
 * Response payload for tariff lookup requests.
 */
public record TariffRateLookupDto(
    String importerIso3,
    String originIso3,
    String hsCode,
    List<TariffRateOptionDto> rates
) {}
