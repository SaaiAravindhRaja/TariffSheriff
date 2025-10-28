package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;

public record TariffCalculationResponse(
    String basis,
    BigDecimal appliedRate,
    BigDecimal totalDuty,
    BigDecimal rvc,
    BigDecimal rvcThreshold
) {}

