package com.tariffsheriff.backend.tariff.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;

@Service
@Transactional(readOnly = true)
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;

    public AgreementServiceImpl(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @Override
    public List<Agreement> getAgreements() {
        return agreementRepository.findAll();
    }

    @Override
    public Agreement getAgreement(Long id) {
        return agreementRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + id));
    }

    @Override
    @Transactional
    public Agreement createAgreement(Agreement agreement) {
        return agreementRepository.save(agreement);
    }

    @Override
    @Transactional
    public Agreement updateAgreement(Long id, Agreement agreement) {
        Agreement existing = getAgreement(id);
        existing.setName(agreement.getName());
        existing.setType(agreement.getType());
        existing.setRvc(agreement.getRvc());
        existing.setStatus(agreement.getStatus());
        existing.setEnteredIntoForce(agreement.getEnteredIntoForce());
        return agreementRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAgreement(Long id) {
        Agreement existing = getAgreement(id);
        agreementRepository.delete(existing);
    }
}
