package com.tariffsheriff.backend.tariff.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tariffsheriff.backend.tariff.model.HsProduct;

public interface HsProductRepository extends JpaRepository<HsProduct, Long>{
    Optional<HsProduct> findByHsCode(String hsCode);
}
