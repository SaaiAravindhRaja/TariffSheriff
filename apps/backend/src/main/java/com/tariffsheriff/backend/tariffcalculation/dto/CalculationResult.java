package com.tariffsheriff.backend.tariffcalculation.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResult {
    private BigDecimal rvcComputed;
    private String rateUsed; // MFN or PREF
    private BigDecimal appliedRate;
    private BigDecimal totalTariff;
}

