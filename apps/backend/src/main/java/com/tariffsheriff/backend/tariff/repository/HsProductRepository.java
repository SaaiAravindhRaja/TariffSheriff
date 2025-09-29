package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tariffsheriff.backend.tariff.model.HsProduct;

@Repository
public interface HsProductRepository extends JpaRepository<HsProduct, Long>{
    Optional<HsProduct> findByHsCode(String hsCode);
}
