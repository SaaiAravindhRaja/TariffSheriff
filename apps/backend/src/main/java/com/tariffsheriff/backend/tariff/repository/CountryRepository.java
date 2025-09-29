package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByIso2IgnoreCase(String iso2);
}
