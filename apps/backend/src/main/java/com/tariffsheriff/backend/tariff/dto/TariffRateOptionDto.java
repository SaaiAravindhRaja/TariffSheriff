package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;

/**
 * Minimal projection of tariff rate data exposed to clients.
 */
public record TariffRateOptionDto(
    Long id,
    String basis,
    BigDecimal adValoremRate,
    boolean nonAdValorem,
    String nonAdValoremText,
    Long agreementId,
    String agreementName,
    BigDecimal rvcThreshold
) {}
