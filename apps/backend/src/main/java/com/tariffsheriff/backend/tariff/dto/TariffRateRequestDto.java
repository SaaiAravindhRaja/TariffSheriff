package com.tariffsheriff.backend.tariff.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TariffRateRequestDto {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal mfnRate;

    // Optional; when provided must be >= 0
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal prefRate;

    // Preferred field name; also accepts legacy "rvc" via alias for compatibility
    @JsonAlias("rvc")
    private BigDecimal rvcThreshold;

    // Unused in current calc logic, kept for future compatibility
    private Long agreementId;

    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal totalValue;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal materialCost;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal labourCost;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal overheadCost;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal profit;

    @NotNull @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal otherCosts;

    @NotNull @Positive
    private BigDecimal fob;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal nonOriginValue;
}
