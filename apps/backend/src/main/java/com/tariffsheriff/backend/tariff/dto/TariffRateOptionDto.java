package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;

/**
 * Minimal projection of tariff rate data exposed to clients.
 */
public record TariffRateOptionDto(
    Long id,
    String basis,
    BigDecimal adValoremRate,
    BigDecimal specificAmount,
    String specificUnit,
    Long agreementId,
    String agreementName,
    BigDecimal rvcThreshold
) {}
