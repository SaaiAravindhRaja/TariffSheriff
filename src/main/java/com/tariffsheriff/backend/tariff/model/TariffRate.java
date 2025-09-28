package com.tariffsheriff.backend.tariff.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TariffRate {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private @Column(nullable = false) Long importerId;
    private @Column(nullable = false) Long originId;
    private @Column(name = "hs_product_id") Long hsCode;
    private @Column(nullable = false) String basis;
    private Long agreementId;
    private @Column(nullable = false) String rateType;
    private @Column(precision = 9, scale = 6) BigDecimal adValoremRate;
    private @Column(precision = 9, scale = 6) BigDecimal specificAmount;
    private @Column(precision = 9, scale = 6) String specificUnit;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String sourceRef;
}