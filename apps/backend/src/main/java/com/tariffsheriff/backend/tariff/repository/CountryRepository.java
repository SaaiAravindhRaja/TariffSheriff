package com.tariffsheriff.backend.tariff.repository;

import com.tariffsheriff.backend.tariff.model.Country;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByIso3IgnoreCase(String iso3);

    Page<Country> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
