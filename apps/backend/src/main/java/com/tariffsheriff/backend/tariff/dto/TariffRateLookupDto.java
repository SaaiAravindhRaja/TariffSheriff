package com.tariffsheriff.backend.tariff.dto;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.model.TariffRate;

public record TariffRateLookupDto(
    TariffRate tariffRateMfn,
    TariffRate tariffRatePref,
    Agreement agreement
) {}
