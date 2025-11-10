package com.tariffsheriff.backend.tariffcalculation.repository;

import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TariffCalculationRepository extends JpaRepository<TariffCalculation, Long> {
    Page<TariffCalculation> findByUser_Id(Long userId, Pageable pageable);
    Optional<TariffCalculation> findByIdAndUser_Id(Long id, Long userId);
    void deleteByIdAndUser_Id(Long id, Long userId);
}

