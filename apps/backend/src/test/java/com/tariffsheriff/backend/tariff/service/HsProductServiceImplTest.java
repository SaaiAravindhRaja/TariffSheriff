package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsProductServiceImplTest {

    @Mock
    HsProductRepository hsProductRepository;

    @InjectMocks
    HsProductServiceImpl svc;

    @Test
    void searchByDescription_returnsEmpty_onNullOrBlank() {
        assertTrue(svc.searchByDescription(null).isEmpty());
        assertTrue(svc.searchByDescription("   ").isEmpty());
    }

    @Test
    void searchByDescription_returnsExactMatches_whenFound() {
        HsProduct p = new HsProduct();
        p.setId(1L);
        when(hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit("hammer", 10)).thenReturn(List.of(p));

        var out = svc.searchByDescription("hammer", 10);
        assertEquals(1, out.size());
        assertEquals(1L, out.get(0).getId());
    }

    @Test
    void searchByDescription_doesFuzzy_whenNoExactMatches() {
        HsProduct p1 = new HsProduct(); p1.setId(2L);
        HsProduct p2 = new HsProduct(); p2.setId(3L);

        // exact returns empty
        when(hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit("steel nails", 5)).thenReturn(List.of());
        // multiple keywords path
        when(hsProductRepository.findByMultipleKeywords("steel", "nails", null)).thenReturn(List.of(p1));
        // second pass by keyword
        when(hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit("steel", 4)).thenReturn(List.of(p1));
        when(hsProductRepository.findByHsLabelContainingIgnoreCaseWithLimit("nails", 4)).thenReturn(List.of(p2));

        var out = svc.searchByDescription("steel nails", 5);
        assertTrue(out.size() >= 1);
        // results should contain products with ids we returned from repository
        assertTrue(out.stream().anyMatch(x -> x.getId().equals(2L)) || out.stream().anyMatch(x -> x.getId().equals(3L)));
    }

    @Test
    void getByHsCode_returnsNull_whenBlankOrNotFound() {
        assertNull(svc.getByHsCode(null));
        assertNull(svc.getByHsCode("   "));
        when(hsProductRepository.findByHsCode("0001")).thenReturn(Optional.empty());
        assertNull(svc.getByHsCode("0001"));
    }

    @Test
    void performFuzzy_filtersStopWords_andLimits() {
        // Use description with many stop words, expect empty keywords -> empty results
        var out = svc.searchByDescription("the and or in on at", 5);
        assertTrue(out.isEmpty());
    }
}
