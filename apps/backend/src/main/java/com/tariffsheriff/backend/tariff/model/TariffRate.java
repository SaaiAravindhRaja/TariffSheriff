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
import com.tariffsheriff.backend.tariff.model.Country;

import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TariffRate {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
@Column(name = "importer_id", nullable = false, unique = true)
private BigInteger importerId;

@Column(name = "origin_id", nullable = false, unique = true)
private BigInteger originId;
    private @Column(name = "hs_product_id") Long hscode;
    private @Column(nullable = false) String basis;
    private Long agreementId;
    private @Column(nullable = false) String rateType;
    private @Column(precision = 9, scale = 6) BigDecimal adValoremRate;
    private @Column(precision = 9, scale = 6) BigDecimal specificAmount;
    private @Column(precision = 9, scale = 6) BigDecimal specificUnit;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String sourceRef;
}