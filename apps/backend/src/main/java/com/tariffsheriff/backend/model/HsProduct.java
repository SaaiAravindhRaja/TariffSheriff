package com.tariffsheriff.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hs_product",
       uniqueConstraints = @UniqueConstraint(name = "uq_hs_product", columnNames = {"destination_id", "hs_version", "hs_code"}))
public class HsProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_id")
    private Country destination;

    @Column(name = "hs_version", nullable = false, length = 20)
    private String hsVersion;

    @Column(name = "hs_code", nullable = false, length = 10)
    private String hsCode;

    @Column(name = "hs_label", nullable = false, length = 255)
    private String hsLabel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Country getDestination() { return destination; }
    public void setDestination(Country destination) { this.destination = destination; }
    public String getHsVersion() { return hsVersion; }
    public void setHsVersion(String hsVersion) { this.hsVersion = hsVersion; }
    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }
    public String getHsLabel() { return hsLabel; }
    public void setHsLabel(String hsLabel) { this.hsLabel = hsLabel; }
}


