package com.tariffsheriff.backend.tariff.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.service.CountryService;

@ExtendWith(MockitoExtension.class)
class CountryControllerTest {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryController controller;

    private Country sampleCountry;
    private List<Country> sampleCountries;
    private Page<Country> samplePage;

    @BeforeEach
    void setUp() {
        // Create sample test data
        sampleCountry = new Country();
        sampleCountry.setId(1L);
        sampleCountry.setIso3("GBR");
        sampleCountry.setName("United Kingdom");

        Country country2 = new Country();
        country2.setId(2L);
        country2.setIso3("USA");
        country2.setName("United States");

        sampleCountries = List.of(sampleCountry, country2);
        samplePage = new PageImpl<>(sampleCountries);
    }

    @Test
    void list_withoutQuery_returnsAllCountries() {
        // Given
        PageRequest expectedPageRequest = PageRequest.of(0, 250);
        when(countryService.searchByName(isNull(), eq(expectedPageRequest)))
            .thenReturn(samplePage);

        // When
        List<Country> result = controller.list(null, 0, 250);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("GBR", result.get(0).getIso3());
        assertEquals("USA", result.get(1).getIso3());
        verify(countryService).searchByName(isNull(), eq(expectedPageRequest));
    }

    @Test
    void list_withQuery_returnsFilteredCountries() {
        // Given
        Page<Country> filteredPage = new PageImpl<>(List.of(sampleCountry));
        when(countryService.searchByName(eq("United K"), any(PageRequest.class)))
            .thenReturn(filteredPage);

        // When
        List<Country> result = controller.list("United K", 0, 250);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("GBR", result.get(0).getIso3());
        assertEquals("United Kingdom", result.get(0).getName());
        verify(countryService).searchByName(eq("United K"), any(PageRequest.class));
    }

    @Test
    void get_existingId_returnsCountry() {
        // Given
        when(countryService.get(1L)).thenReturn(sampleCountry);

        // When
        Country result = controller.get(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("GBR", result.getIso3());
        assertEquals("United Kingdom", result.getName());
        verify(countryService).get(1L);
    }

    @Test
    void get_nonExistingId_throwsException() {
        // Given
        Long nonExistingId = 999L;
        when(countryService.get(nonExistingId))
            .thenThrow(new IllegalArgumentException("Country not found: " + nonExistingId));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> controller.get(nonExistingId));
        verify(countryService).get(nonExistingId);
    }
}