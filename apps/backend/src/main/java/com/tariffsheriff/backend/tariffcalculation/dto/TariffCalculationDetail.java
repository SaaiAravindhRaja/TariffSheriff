package com.tariffsheriff.backend.tariffcalculation.dto;

import java.time.LocalDateTime;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculationDetail {
    private Long id;
    private TariffRateRequestDto input;
    private CalculationResult result;
    private String name;
    private String notes;
    private String hsCode;
    private String importerIso3;
    private String originIso3;
    private LocalDateTime createdAt;
}

