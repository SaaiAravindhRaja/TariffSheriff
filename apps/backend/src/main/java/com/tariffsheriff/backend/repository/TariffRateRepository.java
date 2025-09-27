package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.TariffRate;
import com.tariffsheriff.backend.model.enums.TariffBasis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    @Query("SELECT t FROM TariffRate t WHERE t.importer.id = :importerId AND t.hsProduct.id = :hsProductId AND t.basis = com.tariffsheriff.backend.model.enums.TariffBasis.MFN AND t.origin IS NULL AND t.validFrom <= :shipmentDate AND (t.validTo IS NULL OR t.validTo >= :shipmentDate) ORDER BY t.validFrom DESC")
    java.util.List<TariffRate> findApplicableMfn(@Param("importerId") Long importerId,
                                                @Param("hsProductId") Long hsProductId,
                                                @Param("shipmentDate") java.time.LocalDate shipmentDate);

    @Query("SELECT t FROM TariffRate t WHERE t.importer.id = :importerId AND t.origin.id = :originId AND t.hsProduct.id = :hsProductId AND t.basis = com.tariffsheriff.backend.model.enums.TariffBasis.PREF AND t.validFrom <= :shipmentDate AND (t.validTo IS NULL OR t.validTo >= :shipmentDate) ORDER BY t.validFrom DESC")
    java.util.List<TariffRate> findApplicablePreferential(@Param("importerId") Long importerId,
                                                          @Param("originId") Long originId,
                                                          @Param("hsProductId") Long hsProductId,
                                                          @Param("shipmentDate") java.time.LocalDate shipmentDate);
}


