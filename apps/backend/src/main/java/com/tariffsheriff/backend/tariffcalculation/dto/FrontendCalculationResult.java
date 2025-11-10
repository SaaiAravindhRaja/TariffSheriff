package com.tariffsheriff.backend.tariffcalculation.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class FrontendCalculationResult {
    private BigDecimal totalCost;       // total duty/cost to save
    private String tariffBasis;         // "MFN" or "PREF"
    private BigDecimal appliedRate;     // decimal rate applied
    private BigDecimal calculatedRvc;   // computed RVC percentage
    private BigDecimal rvcThreshold;    // threshold used in the comparison
}

