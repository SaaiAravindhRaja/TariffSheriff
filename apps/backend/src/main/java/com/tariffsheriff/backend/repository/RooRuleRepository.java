package com.tariffsheriff.backend.repository;

import com.tariffsheriff.backend.model.RooRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RooRuleRepository extends JpaRepository<RooRule, Long> {
}


