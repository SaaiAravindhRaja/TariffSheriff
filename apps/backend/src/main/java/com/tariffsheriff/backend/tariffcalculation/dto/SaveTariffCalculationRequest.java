package com.tariffsheriff.backend.tariffcalculation.dto;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import lombok.Data;

@Data
public class SaveTariffCalculationRequest {
    private TariffRateRequestDto input;
    private FrontendCalculationResult result; // trust FE result
    private String name;
    private String notes;
    private String hsCode;
    private String importerIso2;
    private String originIso2;
}
