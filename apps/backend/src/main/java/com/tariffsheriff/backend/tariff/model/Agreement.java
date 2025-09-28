package com.tariffsheriff.backend.tariff.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.*;
@Entity
@Table(name = "agreement")
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

    @Column(nullable = false)
    private Integer rvc;

    @Column(nullable = false)
    private String status;

    @Column(name = "entered_into_force")
    private LocalDate enteredIntoForce;
}
