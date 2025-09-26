package com.tariffsheriff.backend.model;

import com.tariffsheriff.backend.model.converter.TariffBasisConverter;
import com.tariffsheriff.backend.model.converter.TariffRateTypeConverter;
import com.tariffsheriff.backend.model.enums.TariffBasis;
import com.tariffsheriff.backend.model.enums.TariffRateType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariff_rate",
       indexes = {
           @Index(name = "idx_tariff_lookup", columnList = "importer_id,hs_product_id,valid_from DESC"),
           @Index(name = "idx_pref_lookup", columnList = "importer_id,origin_id,hs_product_id,valid_from DESC")
       })
public class TariffRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "importer_id")
    private Country importer;

    @ManyToOne
    @JoinColumn(name = "origin_id")
    private Country origin; // nullable for MFN

    @ManyToOne(optional = false)
    @JoinColumn(name = "hs_product_id")
    private HsProduct hsProduct;

    @Convert(converter = TariffBasisConverter.class)
    @Column(name = "basis", nullable = false, length = 4)
    private TariffBasis basis; // MFN or PREF

    @ManyToOne
    @JoinColumn(name = "agreement_id")
    private Agreement agreement; // nullable unless PREF

    @Convert(converter = TariffRateTypeConverter.class)
    @Column(name = "rate_type", nullable = false, length = 10)
    private TariffRateType rateType; // ad_valorem | specific | compound

    @Column(name = "ad_valorem_rate", precision = 9, scale = 6)
    private BigDecimal adValoremRate;

    @Column(name = "specific_amount", precision = 19, scale = 4)
    private BigDecimal specificAmount;

    @Column(name = "specific_unit", length = 32)
    private String specificUnit;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "source_ref")
    private String sourceRef;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Country getImporter() { return importer; }
    public void setImporter(Country importer) { this.importer = importer; }
    public Country getOrigin() { return origin; }
    public void setOrigin(Country origin) { this.origin = origin; }
    public HsProduct getHsProduct() { return hsProduct; }
    public void setHsProduct(HsProduct hsProduct) { this.hsProduct = hsProduct; }
    public TariffBasis getBasis() { return basis; }
    public void setBasis(TariffBasis basis) { this.basis = basis; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public TariffRateType getRateType() { return rateType; }
    public void setRateType(TariffRateType rateType) { this.rateType = rateType; }
    public BigDecimal getAdValoremRate() { return adValoremRate; }
    public void setAdValoremRate(BigDecimal adValoremRate) { this.adValoremRate = adValoremRate; }
    public BigDecimal getSpecificAmount() { return specificAmount; }
    public void setSpecificAmount(BigDecimal specificAmount) { this.specificAmount = specificAmount; }
    public String getSpecificUnit() { return specificUnit; }
    public void setSpecificUnit(String specificUnit) { this.specificUnit = specificUnit; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
}


