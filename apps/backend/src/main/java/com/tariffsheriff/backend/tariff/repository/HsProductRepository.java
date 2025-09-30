package com.tariffsheriff.backend.tariff.repository;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HsProductRepository extends JpaRepository<HsProduct, Long> {
    Optional<HsProduct> findByHsCode(String hsCode);

    Optional<HsProduct> findByDestinationIdAndHsCode(Long destinationId, String hsCode);
}
