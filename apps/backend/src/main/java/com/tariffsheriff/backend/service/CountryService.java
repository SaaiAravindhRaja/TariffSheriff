package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.Country;
import com.tariffsheriff.backend.repository.CountryRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CountryService {
    private final CountryRepository countryRepository;

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public Page<Country> searchByName(String q, Pageable pageable) {
        return countryRepository.findAllByNameContainingIgnoreCase(q == null ? "" : q, pageable);
    }

    public Country get(Long id) {
        return countryRepository.findById(id).orElseThrow(() -> new NotFoundException("Country not found: " + id));
    }

    @Transactional
    public Country create(Country country) {
        return countryRepository.save(country);
    }

    @Transactional
    public Country update(Long id, Country update) {
        Country existing = get(id);
        existing.setIso2(update.getIso2());
        existing.setIso3(update.getIso3());
        existing.setName(update.getName());
        return countryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        countryRepository.deleteById(id);
    }
}


