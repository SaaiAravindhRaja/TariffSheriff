package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.util.List;

import com.tariffsheriff.backend.tariff.dto.TariffRateLookupDto;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.model.TariffRate;

/**
 * Service interface for tariff rate operations.
 */
public interface TariffRateService {
    
    /**
     * List all tariff rates.
     *
     * @return List of all tariff rates
     */
    List<TariffRate> listTariffRates();
    
    /**
     * Get tariff rate by ID.
     *
     * @param id The tariff rate ID
     * @return The tariff rate
     */
    TariffRate getTariffRateById(Long id);
    
    /**
     * Get tariff rate by importer, origin, HS code, and basis.
     *
     * @param importerId The importer ID
     * @param originId The origin ID
     * @param hsCode The HS code
     * @param basis The basis
     * @return The tariff rate
     */
    TariffRate getTariffRateByImporterAndOriginAndHsCodeAndBasis(
            Long importerId, Long originId, Long hsCode, String basis);
    
    /**
     * Get tariff rate with agreement information.
     *
     * @param importerId The importer ID
     * @param originId The origin ID
     * @param hsCode The HS code
     * @param basis The basis
     * @return The tariff rate lookup DTO
     */
    TariffRateLookupDto getTariffRateWithAgreement(Long importerId, Long originId, Long hsCode, String basis);
    
    /**
     * Calculate tariff rate based on request.
     *
     * @param rq The tariff rate request
     * @return The calculated tariff rate
     */
    BigDecimal calculateTariffRate(TariffRateRequestDto rq);
}