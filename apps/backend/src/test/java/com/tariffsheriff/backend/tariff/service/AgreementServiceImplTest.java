package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.Agreement;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgreementServiceImplTest {

    @Mock
    AgreementRepository agreementRepository;

    @InjectMocks
    AgreementServiceImpl svc;

    @Test
    void list_delegatesToRepository() {
        Page<Agreement> page = new PageImpl<>(List.of(new Agreement(1L, "A", BigDecimal.ZERO)));
        when(agreementRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        Page<Agreement> out = svc.list(PageRequest.of(0, 10));
        assertEquals(1, out.getTotalElements());
    }

    @Test
    void getAgreements_returnsAll() {
        when(agreementRepository.findAll()).thenReturn(List.of(new Agreement(2L, "B", BigDecimal.TEN)));
        var out = svc.getAgreements();
        assertEquals(1, out.size());
        assertEquals("B", out.get(0).getName());
    }

    @Test
    void getAgreement_throwsWhenNotFound() {
        when(agreementRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> svc.getAgreement(5L));
    }

    @Test
    void createAgreement_savesAndReturns() {
        Agreement a = new Agreement(null, "C", new BigDecimal("1.5"));
        Agreement saved = new Agreement(10L, "C", new BigDecimal("1.5"));
        when(agreementRepository.save(a)).thenReturn(saved);

        Agreement out = svc.createAgreement(a);
        assertEquals(10L, out.getId());
    }

    @Test
    void updateAgreement_updatesFields() {
        Agreement existing = new Agreement(20L, "Old", new BigDecimal("2"));
        when(agreementRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(agreementRepository.save(existing)).thenReturn(existing);

        Agreement updated = new Agreement(null, "New", new BigDecimal("3"));
        Agreement out = svc.updateAgreement(20L, updated);
        assertEquals("New", out.getName());
        assertEquals(new BigDecimal("3"), out.getRvcThreshold());
    }

    @Test
    void deleteAgreement_deletesFound() {
        Agreement existing = new Agreement(30L, "X", BigDecimal.ZERO);
        when(agreementRepository.findById(30L)).thenReturn(Optional.of(existing));

        svc.deleteAgreement(30L);
        verify(agreementRepository).delete(existing);
    }

    @Test
    void getAgreementsByCountry_delegates() {
        when(agreementRepository.findAgreementsByCountryIso3("GBR")).thenReturn(List.of());
        var out = svc.getAgreementsByCountry("GBR");
        assertNotNull(out);
    }
}
