package com.tariffsheriff.backend.tariff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tariff_rate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "importer_id", nullable = false)
    private Long importerId;

    @Column(name = "origin_id")
    private Long originId;

    @Column(name = "hs_product_id", nullable = false)
    private Long hsCode;

    @Column(nullable = false)
    private String basis;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "rate_type", nullable = false)
    private String rateType;

    @Column(name = "ad_valorem_rate", precision = 9, scale = 6)
    private BigDecimal adValoremRate;

    @Column(name = "specific_amount", precision = 19, scale = 4)
    private BigDecimal specificAmount;

    @Column(name = "specific_unit", length = 32)
    private String specificUnit;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "source_ref")
    private String sourceRef;
}
