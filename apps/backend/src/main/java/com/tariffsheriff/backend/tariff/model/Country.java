package com.tariffsheriff.backend.tariff.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "country")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

@Column(name = "iso2", columnDefinition = "CHAR(2)", nullable = false, unique = true)
    private String iso2;

    @Column(name = "iso3", columnDefinition = "CHAR(3)", nullable = false, unique = true)
    private String iso3;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

}