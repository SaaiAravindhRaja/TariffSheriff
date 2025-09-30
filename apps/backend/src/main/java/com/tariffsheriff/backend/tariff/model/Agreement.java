package com.tariffsheriff.backend.tariff.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.*;
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @JsonProperty("rvc")
    @Column(name = "rvc_threshold", precision = 5, scale = 2, nullable = false)
    private BigDecimal rvcThreshold;

    @Column(nullable = false)
    private String status;

    @Column(name = "entered_into_force")
    private LocalDate enteredIntoForce;
}
