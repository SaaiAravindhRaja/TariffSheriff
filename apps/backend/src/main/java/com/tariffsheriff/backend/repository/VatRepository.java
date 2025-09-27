package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.Vat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VatRepository extends JpaRepository<Vat, Long> {
    java.util.Optional<Vat> findByImporter_Id(Long importerId);
}


