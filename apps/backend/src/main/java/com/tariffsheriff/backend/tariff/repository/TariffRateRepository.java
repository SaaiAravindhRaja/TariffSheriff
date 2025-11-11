package com.tariffsheriff.backend.tariff.repository;

import java.util.List;
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

    // Get distinct trade routes with counts
    @Query("SELECT tr.importerIso3, tr.originIso3, COUNT(tr.id) " +
            "FROM TariffRate tr " +
            "WHERE tr.originIso3 IS NOT NULL " +
            "GROUP BY tr.importerIso3, tr.originIso3 " +
            "ORDER BY COUNT(tr.id) DESC")
    List<Object[]> findDistinctTradeRoutes();
}
