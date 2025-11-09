package com.tariffsheriff.backend.tariff.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HsProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destination_iso3", nullable = false, columnDefinition = "varchar(3)")
    private String destinationIso3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_iso3", referencedColumnName = "iso3", insertable = false, updatable = false)
    private Country destination;

    @Column(name = "hs_version", length = 20, nullable = false)
    private String hsVersion;

    @Column(name = "hs_code", length = 12, nullable = false)
    private String hsCode;

    @Column(name = "hs_label", length = 255, nullable = false)
    private String hsLabel;
}
