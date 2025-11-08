package com.tariffsheriff.backend.tariff.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "importer_iso3", nullable = false, columnDefinition = "varchar(3)")
    private String importerIso3;

    @Column(name = "origin_iso3", columnDefinition = "varchar(3)")
    private String originIso3;

    @Column(name = "hs_product_id", nullable = false)
    private Long hsProductId;

    @Column(nullable = false)
    private String basis;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "ad_valorem_rate", precision = 9, scale = 6)
    private BigDecimal adValoremRate;

    @Column(name = "is_non_ad_valorem", nullable = false)
    private boolean nonAdValorem;

    @Column(name = "non_ad_valorem_text")
    private String nonAdValoremText;

    @Column(name = "source_ref")
    private String sourceRef;
}
