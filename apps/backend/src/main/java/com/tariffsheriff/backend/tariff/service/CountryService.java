package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CountryService {

    private final CountryRepository countryRepository;

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public Page<Country> searchByName(String query, Pageable pageable) {
        String value = query == null ? "" : query;
        return countryRepository.findAllByNameContainingIgnoreCase(value, pageable);
    }

    public Country get(Long id) {
        return countryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Country not found: " + id));
    }

    public Country create(Country country) {
        return countryRepository.save(country);
    }

    public Country update(Long id, Country update) {
        Country existing = get(id);
        existing.setIso2(update.getIso2());
        existing.setIso3(update.getIso3());
        existing.setName(update.getName());
        return countryRepository.save(existing);
    }

    public void delete(Long id) {
        countryRepository.deleteById(id);
    }

    public List<Country> findAll() {
        return countryRepository.findAll();
    }
}
