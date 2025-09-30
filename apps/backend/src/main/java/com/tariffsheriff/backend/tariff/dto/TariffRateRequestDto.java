package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TariffRateRequestDto {
    private BigDecimal mfnRate;
    private BigDecimal prefRate;
    private BigDecimal rvc;
    private Long agreementId;
    private Integer quantity;
    private BigDecimal totalValue;
    private BigDecimal materialCost;
    private BigDecimal labourCost;
    private BigDecimal overheadCost;
    private BigDecimal profit;
    private BigDecimal otherCosts;
    private BigDecimal fob;
    private BigDecimal nonOriginValue;
}