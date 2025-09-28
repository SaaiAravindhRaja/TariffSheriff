package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.Vat;
import com.tariffsheriff.backend.model.Country;
import com.tariffsheriff.backend.repository.CountryRepository;
import com.tariffsheriff.backend.repository.VatRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class VatService {
    private final VatRepository vatRepository;
    private final CountryRepository countryRepository;

    public VatService(VatRepository vatRepository, CountryRepository countryRepository) {
        this.vatRepository = vatRepository;
        this.countryRepository = countryRepository;
    }

    public Vat getForImporter(Long importerId) {
        return vatRepository.findByImporter_Id(importerId)
            .orElseThrow(() -> new NotFoundException("VAT not found for importer: " + importerId));
    }

    @Transactional
    public Vat upsert(Long importerId, BigDecimal standardRate) {
        return vatRepository.findById(importerId)
            .map(existing -> {
                existing.setStandardRate(standardRate);
                return vatRepository.save(existing);
            })
            .orElseGet(() -> {
                Country importer = countryRepository.findById(importerId)
                    .orElseThrow(() -> new NotFoundException("Importer country not found: " + importerId));
                Vat vat = new Vat();
                // Do NOT set importerId directly for a new entity; set the association so @MapsId can assign the PK
                vat.setImporter(importer);
                vat.setStandardRate(standardRate);
                return vatRepository.save(vat);
            });
    }
}


