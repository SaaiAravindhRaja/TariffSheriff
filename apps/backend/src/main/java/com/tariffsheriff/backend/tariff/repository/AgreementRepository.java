package com.tariffsheriff.backend.tariff.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.Agreement;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

}
