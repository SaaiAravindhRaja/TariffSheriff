package com.tariffsheriff.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vat")
public class Vat {
    @Id
    @Column(name = "importer_id")
    private Long importerId; // references country.id; simple PK mapping

    @OneToOne
    @MapsId
    @JoinColumn(name = "importer_id")
    private Country importer;

    @Column(name = "standard_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal standardRate;

    public Long getImporterId() { return importerId; }
    public void setImporterId(Long importerId) { this.importerId = importerId; }
    public Country getImporter() { return importer; }
    public void setImporter(Country importer) { this.importer = importer; }
    public BigDecimal getStandardRate() { return standardRate; }
    public void setStandardRate(BigDecimal standardRate) { this.standardRate = standardRate; }
}


