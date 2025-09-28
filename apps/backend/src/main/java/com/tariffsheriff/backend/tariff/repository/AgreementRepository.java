package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.TariffRate;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

}
