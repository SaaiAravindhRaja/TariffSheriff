package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.TariffRate;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

    Optional<TariffRate> findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(
        String importerIso3, String originIso3, Long hsProductId, String basis);

    Optional<TariffRate> findByImporterIso3AndHsProductIdAndBasis(
        String importerIso3, Long hsProductId, String basis);
}
