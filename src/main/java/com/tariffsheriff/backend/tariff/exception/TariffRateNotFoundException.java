package com.tariffsheriff.backend.tariff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TariffRateNotFoundException extends RuntimeException {
    public TariffRateNotFoundException() {
        super("Could not find tariff rate information");
    }
}