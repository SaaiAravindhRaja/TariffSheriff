package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
}


