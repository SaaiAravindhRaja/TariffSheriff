package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.Agreement;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgreementService {

    Page<Agreement> list(Pageable pageable);

    List<Agreement> getAgreements();

    Agreement getAgreement(Long id);

    Agreement createAgreement(Agreement agreement);

    Agreement updateAgreement(Long id, Agreement agreement);

    void deleteAgreement(Long id);

    List<Agreement> getAgreementsByCountry(String countryIso3);
}
