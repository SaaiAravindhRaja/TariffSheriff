package com.tariffsheriff.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tariffsheriff.backend.model.converter.AgreementStatusConverter;
import com.tariffsheriff.backend.model.enums.AgreementStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "agreement")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String type;

    @Convert(converter = AgreementStatusConverter.class)
    @Column(nullable = false, length = 20)
    private AgreementStatus status;

    @Column(name = "entered_into_force")
    private LocalDate enteredIntoForce;

    // Many-to-many with Country through agreement_party join table
    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "agreement_party",
            joinColumns = @JoinColumn(name = "agreement_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> parties = new HashSet<>();

    @Column(name = "rvc_threshold", precision = 5, scale = 2)
    private java.math.BigDecimal rvcThreshold;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public AgreementStatus getStatus() { return status; }
    public void setStatus(AgreementStatus status) { this.status = status; }
    public LocalDate getEnteredIntoForce() { return enteredIntoForce; }
    public void setEnteredIntoForce(LocalDate enteredIntoForce) { this.enteredIntoForce = enteredIntoForce; }
    public Set<Country> getParties() { return parties; }
    public void setParties(Set<Country> parties) { this.parties = parties; }
    public java.math.BigDecimal getRvcThreshold() { return rvcThreshold; }
    public void setRvcThreshold(java.math.BigDecimal rvcThreshold) { this.rvcThreshold = rvcThreshold; }
}


