package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;

    public AgreementServiceImpl(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @Override
    public Page<Agreement> list(Pageable pageable) {
        return agreementRepository.findAll(pageable);
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
        existing.setRvcThreshold(agreement.getRvcThreshold());
        return agreementRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAgreement(Long id) {
        Agreement existing = getAgreement(id);
        agreementRepository.delete(existing);
    }

    @Override
    public List<Agreement> getAgreementsByCountry(String countryIso3) {
        return agreementRepository.findAgreementsByCountryIso3(countryIso3);
    }
}
