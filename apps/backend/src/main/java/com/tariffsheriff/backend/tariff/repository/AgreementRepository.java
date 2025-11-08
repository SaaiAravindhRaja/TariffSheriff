package com.tariffsheriff.backend.tariff.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.Agreement;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    @Query(value = "SELECT DISTINCT a.* FROM agreement a " +
           "JOIN agreement_party ap ON a.id = ap.agreement_id " +
           "WHERE UPPER(ap.country_iso3) = UPPER(:countryIso3)", nativeQuery = true)
    List<Agreement> findAgreementsByCountryIso3(@Param("countryIso3") String countryIso3);

}
