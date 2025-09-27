package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    Page<Country> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}


