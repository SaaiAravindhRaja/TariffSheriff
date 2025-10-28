package com.tariffsheriff.backend.tariff.controller;

import java.math.BigDecimal;
import java.util.List;

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
import com.tariffsheriff.backend.tariff.service.TariffRateService;

@RestController
@RequestMapping("/api/tariff-rate")
public class TariffRateController {
    private TariffRateService tariffRateService;

    public TariffRateController(TariffRateService trs) {
        this.tariffRateService = trs;
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
    public TariffRateLookupDto getTariffRateAndAgreement(@RequestParam String importerIso2,
                                                         @RequestParam(required = false) String originIso2,
                                                         @RequestParam String hsCode) {
        return tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
    }

    @PostMapping("/calculate")
    public TariffCalculationResponse calculateTariffRate(@jakarta.validation.Valid @RequestBody TariffRateRequestDto tariffCalculationData) {
        return tariffRateService.calculateTariffRate(tariffCalculationData);
    }


    

}
