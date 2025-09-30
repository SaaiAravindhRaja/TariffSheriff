package com.tariffsheriff.backend.tariff.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.Agreement;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    @Query("SELECT DISTINCT a FROM Agreement a " +
           "JOIN agreement_party ap ON a.id = ap.agreement_id " +
           "JOIN Country c ON ap.country_id = c.id " +
           "WHERE UPPER(c.iso2) = UPPER(:countryIso2)")
    List<Agreement> findAgreementsByCountryIso2(@Param("countryIso2") String countryIso2);

}
