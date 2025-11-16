package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.service.HsProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hs-products")
public class HsProductController {

    private final HsProductService hsProductService;
    private final HsProductRepository hsProductRepository;

    public HsProductController(HsProductService hsProductService, HsProductRepository hsProductRepository) {
        this.hsProductService = hsProductService;
        this.hsProductRepository = hsProductRepository;
    }

    /**
     * Simple search endpoint combining code prefix and description search.
     * Returns up to `limit` items with minimal fields: hsCode and hsLabel.
     */
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "200") int limit,
            @RequestParam(value = "importerIso3", required = false) String importerIso3
    ) {
        String q = query == null ? "" : query.trim();
        int capped = Math.max(1, Math.min(limit, 200));
        List<HsProduct> results = new ArrayList<>();

        // If destination is selected and no query typed, return initial list for that destination
        if ((q.isEmpty() || q.isBlank()) && importerIso3 != null && !importerIso3.isBlank()) {
            results.addAll(hsProductRepository.findByDestinationWithLimit(importerIso3.trim().toUpperCase(), capped));
        } else if (!q.isEmpty()) {
            boolean isDigitsOnly = q.chars().allMatch(Character::isDigit);
            if (importerIso3 != null && !importerIso3.isBlank()) {
                String iso3 = importerIso3.trim().toUpperCase();
                if (isDigitsOnly) {
                    // dot-insensitive digits match first
                    results.addAll(hsProductRepository.findByDestinationAndHsCodeDigitsPrefix(iso3, q, capped));
                    // then literal prefix as fallback
                    if (results.size() < capped) {
                        results.addAll(hsProductRepository.findByDestinationAndHsCodePrefix(iso3, q, capped));
                    }
                } else {
                    // description search filtered by destination
                    List<HsProduct> desc = hsProductService.searchByDescription(q, capped);
                    for (HsProduct p : desc) {
                        if (iso3.equalsIgnoreCase(p.getDestinationIso3())) {
                            results.add(p);
                            if (results.size() >= capped) break;
                        }
                    }
                }
            } else {
                // No destination filter
                if (isDigitsOnly) {
                    results.addAll(hsProductRepository.findByHsCodePrefix(q, capped));
                }
                if (results.size() < capped) {
                    List<HsProduct> desc = hsProductService.searchByDescription(q, capped);
                    for (HsProduct p : desc) {
                        if (results.stream().noneMatch(x -> x.getId().equals(p.getId()))) {
                            results.add(p);
                            if (results.size() >= capped) break;
                        }
                    }
                }
            }
        }

        // Map to minimal DTO
        List<Map<String, Object>> out = new ArrayList<>();
        for (HsProduct p : results) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hsCode", p.getHsCode());
            m.put("hsLabel", p.getHsLabel());
            out.add(m);
        }
        return out;
    }
}

