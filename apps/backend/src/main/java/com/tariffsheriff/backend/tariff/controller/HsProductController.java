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
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return List.of();

        int capped = Math.max(1, Math.min(limit, 25));
        List<HsProduct> results = new ArrayList<>();

        boolean looksNumeric = q.chars().allMatch(Character::isDigit);
        if (looksNumeric) {
            results.addAll(hsProductRepository.findByHsCodePrefix(q, capped));
        }
        if (results.size() < capped) {
            // fill via description search
            List<HsProduct> desc = hsProductService.searchByDescription(q, capped);
            for (HsProduct p : desc) {
                if (results.stream().noneMatch(x -> x.getId().equals(p.getId()))) {
                    results.add(p);
                    if (results.size() >= capped) break;
                }
            }
        }

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

