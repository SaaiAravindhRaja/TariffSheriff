package com.tariffsheriff.backend.tariff.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Filter by country pair (e.g., India → Singapore)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.originIso3 = :originIso3")
    List<TariffRate> findByCountryPair(@Param("importerIso3") String importerIso3,
            @Param("originIso3") String originIso3,
            Pageable pageable);

    // Filter by country pair + HS code prefix (using subquery)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.originIso3 = :originIso3 " +
            "AND tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE hp.hsCode LIKE CONCAT(:hsCodePrefix, '%'))")
    List<TariffRate> findByCountryPairAndHsCode(@Param("importerIso3") String importerIso3,
            @Param("originIso3") String originIso3,
            @Param("hsCodePrefix") String hsCodePrefix,
            Pageable pageable);

    // Filter by country pair + multiple HS code prefixes (for categories with
    // multiple codes)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.originIso3 = :originIso3 " +
            "AND tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE " +
            "hp.hsCode LIKE CONCAT(:hsPrefix1, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsPrefix2, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsPrefix3, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsPrefix4, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsPrefix5, '%'))")
    List<TariffRate> findByCountryPairAndMultipleHsCodes(
            @Param("importerIso3") String importerIso3,
            @Param("originIso3") String originIso3,
            @Param("hsPrefix1") String hsPrefix1,
            @Param("hsPrefix2") String hsPrefix2,
            @Param("hsPrefix3") String hsPrefix3,
            @Param("hsPrefix4") String hsPrefix4,
            @Param("hsPrefix5") String hsPrefix5,
            Pageable pageable);

    // Filter by importer only
    @Query("SELECT tr FROM TariffRate tr WHERE tr.importerIso3 = :importerIso3")
    List<TariffRate> findByImporter(@Param("importerIso3") String importerIso3, Pageable pageable);

    // Filter by importer + HS code prefix (using subquery)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE hp.hsCode LIKE CONCAT(:hsCodePrefix, '%'))")
    List<TariffRate> findByImporterAndHsCode(@Param("importerIso3") String importerIso3,
            @Param("hsCodePrefix") String hsCodePrefix,
            Pageable pageable);

    // Filter by country pair + list of HS codes (with prefix matching)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.originIso3 = :originIso3 " +
            "AND tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE " +
            "hp.hsCode LIKE CONCAT(:hsCode1, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode2, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode3, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode4, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode5, '%'))")
    List<TariffRate> findByCountryPairAndHsCodes(
            @Param("importerIso3") String importerIso3,
            @Param("originIso3") String originIso3,
            @Param("hsCode1") String hsCode1,
            @Param("hsCode2") String hsCode2,
            @Param("hsCode3") String hsCode3,
            @Param("hsCode4") String hsCode4,
            @Param("hsCode5") String hsCode5,
            Pageable pageable);

    // Filter by importer + list of HS codes (with prefix matching)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.importerIso3 = :importerIso3 " +
            "AND tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE " +
            "hp.hsCode LIKE CONCAT(:hsCode1, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode2, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode3, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode4, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode5, '%'))")
    List<TariffRate> findByImporterAndHsCodes(
            @Param("importerIso3") String importerIso3,
            @Param("hsCode1") String hsCode1,
            @Param("hsCode2") String hsCode2,
            @Param("hsCode3") String hsCode3,
            @Param("hsCode4") String hsCode4,
            @Param("hsCode5") String hsCode5,
            Pageable pageable);

    // Filter by list of HS codes only (with prefix matching)
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE " +
            "hp.hsCode LIKE CONCAT(:hsCode1, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode2, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode3, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode4, '%') OR " +
            "hp.hsCode LIKE CONCAT(:hsCode5, '%'))")
    List<TariffRate> findByHsCodes(
            @Param("hsCode1") String hsCode1,
            @Param("hsCode2") String hsCode2,
            @Param("hsCode3") String hsCode3,
            @Param("hsCode4") String hsCode4,
            @Param("hsCode5") String hsCode5,
            Pageable pageable);

    // Filter by single HS code prefix
    @Query("SELECT tr FROM TariffRate tr " +
            "WHERE tr.hsProductId IN (SELECT hp.id FROM HsProduct hp WHERE hp.hsCode LIKE CONCAT(:hsCodePrefix, '%'))")
    List<TariffRate> findByHsCodePrefix(
            @Param("hsCodePrefix") String hsCodePrefix,
            Pageable pageable);
}
