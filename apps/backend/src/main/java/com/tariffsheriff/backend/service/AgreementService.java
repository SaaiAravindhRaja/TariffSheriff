package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.Agreement;
import com.tariffsheriff.backend.repository.AgreementRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AgreementService {
    private final AgreementRepository agreementRepository;

    public AgreementService(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    public Page<Agreement> list(Pageable pageable) { return agreementRepository.findAll(pageable); }

    public Agreement get(Long id) { return agreementRepository.findById(id).orElseThrow(() -> new NotFoundException("Agreement not found: " + id)); }

    @Transactional
    public Agreement create(Agreement agreement) { return agreementRepository.save(agreement); }

    @Transactional
    public Agreement update(Long id, Agreement update) {
        Agreement existing = get(id);
        existing.setName(update.getName());
        existing.setType(update.getType());
        existing.setStatus(update.getStatus());
        existing.setEnteredIntoForce(update.getEnteredIntoForce());
        existing.setParties(update.getParties());
        existing.setRvcThreshold(update.getRvcThreshold());
        return agreementRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) { agreementRepository.deleteById(id); }
}


