package com.tariffsheriff.backend.tariffcalculation.repository;

import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TariffCalculationRepository extends JpaRepository<TariffCalculation, Long> {
    Page<TariffCalculation> findByUser_Id(Long userId, Pageable pageable);

    Optional<TariffCalculation> findByIdAndUser_Id(Long id, Long userId);

    void deleteByIdAndUser_Id(Long id, Long userId);

    // Dashboard stats queries
    @Query("SELECT COALESCE(SUM(tc.totalTariff), 0) FROM TariffCalculation tc WHERE tc.user.id = :userId")
    BigDecimal sumTotalTariffByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(tc) FROM TariffCalculation tc WHERE tc.user.id = :userId AND tc.createdAt >= :startDate")
    Long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT tc.hsCode, COUNT(tc) as freq FROM TariffCalculation tc WHERE tc.user.id = :userId AND tc.hsCode IS NOT NULL GROUP BY tc.hsCode ORDER BY freq DESC")
    java.util.List<Object[]> findMostUsedHsCodeByUserId(@Param("userId") Long userId, Pageable pageable);
}
