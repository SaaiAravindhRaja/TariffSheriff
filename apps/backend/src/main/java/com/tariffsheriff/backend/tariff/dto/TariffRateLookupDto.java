package com.tariffsheriff.backend.tariff.dto;

import java.util.List;

/**
 * Response payload for tariff lookup requests.
 */
public record TariffRateLookupDto(
    String importerIso2,
    String originIso2,
    String hsCode,
    List<TariffRateOptionDto> rates
) {}
