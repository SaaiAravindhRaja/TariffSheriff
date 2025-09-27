package com.tariffsheriff.backend.service;

import com.tariffsheriff.backend.model.HsProduct;
import com.tariffsheriff.backend.repository.HsProductRepository;
import com.tariffsheriff.backend.service.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HsProductService {
    private final HsProductRepository hsProductRepository;

    public HsProductService(HsProductRepository hsProductRepository) {
        this.hsProductRepository = hsProductRepository;
    }

    public Page<HsProduct> listByDestination(Long destinationId, Pageable pageable) {
        return hsProductRepository.findByDestination_Id(destinationId, pageable);
    }

    public HsProduct get(Long id) {
        return hsProductRepository.findById(id).orElseThrow(() -> new NotFoundException("HS product not found: " + id));
    }

    @Transactional
    public HsProduct create(HsProduct hsProduct) { return hsProductRepository.save(hsProduct); }

    @Transactional
    public HsProduct update(Long id, HsProduct update) {
        HsProduct existing = get(id);
        existing.setDestination(update.getDestination());
        existing.setHsVersion(update.getHsVersion());
        existing.setHsCode(update.getHsCode());
        existing.setHsLabel(update.getHsLabel());
        return hsProductRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) { hsProductRepository.deleteById(id); }
}


