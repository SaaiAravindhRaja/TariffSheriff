package com.tariffsheriff.backend.tariff.dto;

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.Data;

@Data
public class TariffRateRequestDto {
    private Long importer_id; // should change to accept wtv is stashed on frontend and sent in payload
                                // can be ids/code or just mfnrate and prefrate
    private Long origin_id;
    private Long hsCode;
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