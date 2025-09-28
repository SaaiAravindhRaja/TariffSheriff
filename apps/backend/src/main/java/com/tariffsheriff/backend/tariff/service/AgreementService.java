package com.tariffsheriff.backend.tariff.service;

import java.util.List;

import com.tariffsheriff.backend.tariff.model.Agreement;

public interface AgreementService {

    List<Agreement> getAgreements();

    Agreement getAgreement(Long id);

    Agreement createAgreement(Agreement agreement);

    Agreement updateAgreement(Long id, Agreement agreement);

    void deleteAgreement(Long id);
}
