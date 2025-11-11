package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.TariffRate;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

    Optional<TariffRate> findByImporterIso3AndOriginIso3AndHsProductIdAndBasis(
            String importerIso3, String originIso3, Long hsProductId, String basis);

    Optional<TariffRate> findByImporterIso3AndHsProductIdAndBasis(
            String importerIso3, Long hsProductId, String basis);

    // Count distinct trade routes (importer + origin + product combinations)
    @Query("SELECT COUNT(DISTINCT CONCAT(tr.importerIso3, '-', COALESCE(tr.originIso3, 'MFN'), '-', tr.hsProductId)) FROM TariffRate tr")
    Long countDistinctTradeRoutes();
}
