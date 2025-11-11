package com.tariffsheriff.backend.tariff.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.service.AgreementService;

@ExtendWith(MockitoExtension.class)
class AgreementControllerTest {

    @Mock
    private AgreementService agreementService;

    @InjectMocks
    private AgreementController controller;

    private Agreement sampleAgreement;
    private List<Agreement> sampleAgreements;
    private Page<Agreement> samplePage;

    @BeforeEach
    void setUp() {
        // Create sample test data
        sampleAgreement = new Agreement();
        sampleAgreement.setId(1L);
        sampleAgreement.setName("Comprehensive and Progressive Trans-Pacific Partnership");
        sampleAgreement.setRvcThreshold(new BigDecimal("60.00"));

        Agreement agreement2 = new Agreement();
        agreement2.setId(2L);
        agreement2.setName("United States-Mexico-Canada Agreement");
        agreement2.setRvcThreshold(new BigDecimal("62.50"));

        sampleAgreements = List.of(sampleAgreement, agreement2);
        samplePage = new PageImpl<>(sampleAgreements);
    }

    @Test
    void list_returnsAllAgreements() {
        // Given
        when(agreementService.list(any(PageRequest.class)))
            .thenReturn(samplePage);

        // When
        List<Agreement> result = controller.list(0, 250);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Comprehensive and Progressive Trans-Pacific Partnership", result.get(0).getName());
        assertEquals(new BigDecimal("60.00"), result.get(0).getRvcThreshold());
        assertEquals("United States-Mexico-Canada Agreement", result.get(1).getName());
        assertEquals(new BigDecimal("62.50"), result.get(1).getRvcThreshold());
        verify(agreementService).list(any(PageRequest.class));
    }

    @Test
    void get_existingId_returnsAgreement() {
        // Given
        when(agreementService.getAgreement(1L)).thenReturn(sampleAgreement);

        // When
        Agreement result = controller.get(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Comprehensive and Progressive Trans-Pacific Partnership", result.getName());
        assertEquals(new BigDecimal("60.00"), result.getRvcThreshold());
        verify(agreementService).getAgreement(1L);
    }

    @Test
    void getByCountry_returnsAgreementsForCountry() {
        // Given
        String countryIso3 = "JPN";
        when(agreementService.getAgreementsByCountry(countryIso3))
            .thenReturn(List.of(sampleAgreement));

        // When
        List<Agreement> result = controller.getByCountry(countryIso3);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Comprehensive and Progressive Trans-Pacific Partnership", result.get(0).getName());
        assertEquals(new BigDecimal("60.00"), result.get(0).getRvcThreshold());
        verify(agreementService).getAgreementsByCountry(countryIso3);
    }
}