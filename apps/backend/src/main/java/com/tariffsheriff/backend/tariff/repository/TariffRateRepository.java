package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.TariffRate;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

    Optional<TariffRate> findByImporterIdAndOriginIdAndHsCodeAndBasis(
        Long importerId, Long originId, Long hsCode, String basis);

    Optional<TariffRate> findByImporterIdAndHsCodeAndBasis(
        Long importerId, Long hsCode, String basis);
}
