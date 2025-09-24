package com.tariffsheriff.backend.tariff.repository;

import java.math.BigInteger;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.TariffRate;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    Optional<TariffRate> findByImporterIdAndOriginIdAndHscodeAndBasis(BigInteger importer_id, BigInteger origin_id, Long hsCode, String basis);
}