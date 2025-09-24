package com.tariffsheriff.backend.tariff.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.service.TariffRateService;
@RestController
@RequestMapping("/api/tariffs")
public class TariffRateController {
    private TariffRateService tariffRateService;

    public TariffRateController(TariffRateService trs) {
        this.tariffRateService = trs;
    }

    @GetMapping("/tariff-rate")
    public List<TariffRate> getTariffRates() {
        return tariffRateService.listTariffRates();
    }

    @GetMapping("/tariff-rate/{id}")
    public TariffRate getTariffRate(@PathVariable Long id) {
        return tariffRateService.getTariffRateById(id);
    }

    @PostMapping("/calculate")
    public BigDecimal calculateTariffRate(@RequestBody TariffRateRequestDto tariffCalculationData) {
        return tariffRateService.calculateTariffRate(tariffCalculationData);
    }


    

}