package com.tariffsheriff.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "country")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso2", nullable = false, unique = true, columnDefinition = "char(2)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String iso2;

    @Column(name = "iso3", nullable = false, unique = true, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String iso3;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIso2() { return iso2; }
    public void setIso2(String iso2) { this.iso2 = iso2; }
    public String getIso3() { return iso3; }
    public void setIso3(String iso3) { this.iso3 = iso3; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


