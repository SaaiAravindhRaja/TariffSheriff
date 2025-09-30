package com.tariffsheriff.backend.country.controller;

import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.service.CountryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    // q is an optional name filter (substring match), page/size control pagination
    @GetMapping
    public List<Country> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "250") int size
    ) {
        Page<Country> result = countryService.searchByName(q, PageRequest.of(page, size));
        return result.getContent();
    }

    @GetMapping("/{id}")
    public Country get(@PathVariable Long id) {
        return countryService.get(id);
    }
}
