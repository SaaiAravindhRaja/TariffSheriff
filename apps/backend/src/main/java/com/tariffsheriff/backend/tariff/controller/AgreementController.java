package com.tariffsheriff.backend.tariff.controller;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agreements")
public class AgreementController {

    private final AgreementService agreementService;

    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @GetMapping
    public List<Agreement> list(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "250") int size
    ) {
        Page<Agreement> result = agreementService.list(PageRequest.of(page, size));
        return result.getContent();
    }

    @GetMapping("/{id}")
    public Agreement get(@PathVariable Long id) {
        return agreementService.getAgreement(id);
    }

    @GetMapping("/by-country/{countryIso2}")
    public List<Agreement> getByCountry(@PathVariable String countryIso2) {
        return agreementService.getAgreementsByCountry(countryIso2);
    }
}
