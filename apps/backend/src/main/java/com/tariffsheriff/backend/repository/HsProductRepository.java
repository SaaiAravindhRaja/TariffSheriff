package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.HsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HsProductRepository extends JpaRepository<HsProduct, Long> {
    java.util.Optional<HsProduct> findByDestination_IdAndHsVersionAndHsCode(Long destinationId, String hsVersion, String hsCode);
}


