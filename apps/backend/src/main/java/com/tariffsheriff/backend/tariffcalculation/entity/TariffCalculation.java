package com.tariffsheriff.backend.tariffcalculation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tariffsheriff.backend.auth.entity.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tariff_calculation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Metadata
    @Column(name = "name")
    private String name;

    @Column(name = "notes")
    private String notes;

    @Column(name = "hs_code")
    private String hsCode;

    @Column(name = "importer_iso2")
    private String importerIso2;

    @Column(name = "origin_iso2")
    private String originIso2;

    // Inputs from TariffRateRequestDto
    @Column(name = "mfn_rate", precision = 18, scale = 6)
    private BigDecimal mfnRate;

    @Column(name = "pref_rate", precision = 18, scale = 6)
    private BigDecimal prefRate;

    @Column(name = "rvc", precision = 18, scale = 6)
    private BigDecimal rvc;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_value", precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "material_cost", precision = 18, scale = 2)
    private BigDecimal materialCost;

    @Column(name = "labour_cost", precision = 18, scale = 2)
    private BigDecimal labourCost;

    @Column(name = "overhead_cost", precision = 18, scale = 2)
    private BigDecimal overheadCost;

    @Column(name = "profit", precision = 18, scale = 2)
    private BigDecimal profit;

    @Column(name = "other_costs", precision = 18, scale = 2)
    private BigDecimal otherCosts;

    @Column(name = "fob", precision = 18, scale = 2)
    private BigDecimal fob;

    @Column(name = "non_origin_value", precision = 18, scale = 2)
    private BigDecimal nonOriginValue;

    // Computed results
    @Column(name = "rvc_computed", precision = 18, scale = 6)
    private BigDecimal rvcComputed;

    @Column(name = "rate_used", length = 8)
    private String rateUsed; // "MFN" or "PREF"

    @Column(name = "applied_rate", precision = 18, scale = 6)
    private BigDecimal appliedRate;

    @Column(name = "total_tariff", precision = 18, scale = 2)
    private BigDecimal totalTariff;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

