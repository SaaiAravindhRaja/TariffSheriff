package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.Data;

@Data
public class TariffRateRequestDto {
    // private Long agreementId;
    private BigInteger importer_id;
    private BigInteger origin_id;
    private Long hsCode;
    private Integer quantity;
    private BigDecimal totalValue;
    private BigDecimal materialCost;
    private BigDecimal labourCost;
    private BigDecimal overheadCost;
    private BigDecimal profit;
    private BigDecimal otherCosts;
    private BigDecimal FOB;
    private BigDecimal nonOriginValue;
}