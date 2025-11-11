package com.tariffsheriff.backend.tariff.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;
import com.tariffsheriff.backend.tariff.service.TariffRateService;

@RestController
@RequestMapping("/api/tariff-rate")
public class TariffRateController {
    private TariffRateService tariffRateService;
    private TariffRateRepository tariffRateRepository;

    public TariffRateController(TariffRateService trs, TariffRateRepository trr) {
        this.tariffRateService = trs;
        this.tariffRateRepository = trr;
    }

    @GetMapping("/")
    public List<TariffRate> getTariffRates() {
        return tariffRateService.listTariffRates();
    }

    @GetMapping("/{id}")
    public TariffRate getTariffRate(@PathVariable Long id) {
        return tariffRateService.getTariffRateById(id);
    }

    @GetMapping("/lookup")
    public TariffRateLookupDto getTariffRateAndAgreement(@RequestParam String importerIso3,
                                                         @RequestParam(required = false) String originIso3,
                                                         @RequestParam String hsCode) {
        return tariffRateService.getTariffRateWithAgreement(importerIso3, originIso3, hsCode);
    }

    @PostMapping("/calculate")
    public TariffCalculationResponse calculateTariffRate(@jakarta.validation.Valid @RequestBody TariffRateRequestDto tariffCalculationData) {
        return tariffRateService.calculateTariffRate(tariffCalculationData);
    }

    @GetMapping("/routes")
    public List<Map<String, Object>> getTradeRoutes() {
        List<Object[]> routes = tariffRateRepository.findDistinctTradeRoutes();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Object[] route : routes) {
            Map<String, Object> routeMap = new HashMap<>();
            routeMap.put("importerIso3", route[0]);
            routeMap.put("originIso3", route[1]);
            routeMap.put("count", route[2]);
            result.add(routeMap);
        }
        
        return result;
    }
}