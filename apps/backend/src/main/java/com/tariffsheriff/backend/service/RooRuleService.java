package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.RooRule;
import com.tariffsheriff.backend.repository.RooRuleRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RooRuleService {
    private final RooRuleRepository rooRuleRepository;

    public RooRuleService(RooRuleRepository rooRuleRepository) {
        this.rooRuleRepository = rooRuleRepository;
    }

    public Page<RooRule> list(Pageable pageable) { return rooRuleRepository.findAll(pageable); }

    public RooRule get(Long id) { return rooRuleRepository.findById(id).orElseThrow(() -> new NotFoundException("ROO rule not found: " + id)); }

    @Transactional
    public RooRule create(RooRule rule) { return rooRuleRepository.save(rule); }

    @Transactional
    public RooRule update(Long id, RooRule update) {
        RooRule existing = get(id);
        existing.setAgreement(update.getAgreement());
        existing.setHsProduct(update.getHsProduct());
        existing.setMethod(update.getMethod());
        existing.setThreshold(update.getThreshold());
        existing.setRequiresCertificate(update.isRequiresCertificate());
        return rooRuleRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) { rooRuleRepository.deleteById(id); }
}


