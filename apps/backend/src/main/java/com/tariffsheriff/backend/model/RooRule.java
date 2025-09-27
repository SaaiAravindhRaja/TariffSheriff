package com.tariffsheriff.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roo_rule",
       uniqueConstraints = @UniqueConstraint(name = "uq_roo_rule_agreement_product", columnNames = {"agreement_id", "hs_product_id"}))
public class RooRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hs_product_id")
    private HsProduct hsProduct;

    @Column(nullable = false, length = 50)
    private String method;

    @Column(nullable = false, length = 50)
    private String threshold;

    @Column(name = "requires_certificate", nullable = false)
    private boolean requiresCertificate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public HsProduct getHsProduct() { return hsProduct; }
    public void setHsProduct(HsProduct hsProduct) { this.hsProduct = hsProduct; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getThreshold() { return threshold; }
    public void setThreshold(String threshold) { this.threshold = threshold; }
    public boolean isRequiresCertificate() { return requiresCertificate; }
    public void setRequiresCertificate(boolean requiresCertificate) { this.requiresCertificate = requiresCertificate; }
}


