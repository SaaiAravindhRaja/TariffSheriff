package com.tariffsheriff.backend.tariffcalculation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationSummary {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private BigDecimal totalTariff; // stores total cost as saved
    private String rateUsed;        // MFN or PREF

    // Extra display fields
    private BigDecimal appliedRate;
    private BigDecimal rvcComputed;
    private BigDecimal rvcThreshold;
    private String hsCode;
    private String importerIso2;
    private String originIso2;
}
