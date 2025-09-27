package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.TariffRate;
import com.tariffsheriff.backend.repository.TariffRateRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TariffRateService {
    private final TariffRateRepository tariffRateRepository;

    public TariffRateService(TariffRateRepository tariffRateRepository) {
        this.tariffRateRepository = tariffRateRepository;
    }

    public Page<TariffRate> listByImporter(Long importerId, Pageable pageable) {
        return tariffRateRepository.findByImporter_Id(importerId, pageable);
    }

    public TariffRate get(Long id) { return tariffRateRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff rate not found: " + id)); }

    public List<TariffRate> findApplicableMfn(Long importerId, Long hsProductId, LocalDate shipmentDate) {
        return tariffRateRepository.findApplicableMfn(importerId, hsProductId, shipmentDate);
    }

    public List<TariffRate> findApplicablePreferential(Long importerId, Long originId, Long hsProductId, LocalDate shipmentDate) {
        return tariffRateRepository.findApplicablePreferential(importerId, originId, hsProductId, shipmentDate);
    }

    @Transactional
    public TariffRate create(TariffRate rate) { return tariffRateRepository.save(rate); }

    @Transactional
    public TariffRate update(Long id, TariffRate update) {
        TariffRate existing = get(id);
        existing.setImporter(update.getImporter());
        existing.setOrigin(update.getOrigin());
        existing.setHsProduct(update.getHsProduct());
        existing.setBasis(update.getBasis());
        existing.setAgreement(update.getAgreement());
        existing.setRateType(update.getRateType());
        existing.setAdValoremRate(update.getAdValoremRate());
        existing.setSpecificAmount(update.getSpecificAmount());
        existing.setSpecificUnit(update.getSpecificUnit());
        existing.setValidFrom(update.getValidFrom());
        existing.setValidTo(update.getValidTo());
        existing.setSourceRef(update.getSourceRef());
        return tariffRateRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) { tariffRateRepository.deleteById(id); }
}


