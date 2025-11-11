package com.tariffsheriff.backend.tariff.service;

import com.tariffsheriff.backend.tariff.model.Country;
import com.tariffsheriff.backend.tariff.repository.CountryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CountryService countryService;

    private Country usa;
    private Country canada;

    @BeforeEach
    void setUp() {
        usa = new Country(1L, "USA", "United States");
        canada = new Country(2L, "CAN", "Canada");
    }

    @Test
    void searchByName_ShouldReturnAllCountries_WhenQueryIsNull() {
        // Arrange
        List<Country> countries = Arrays.asList(usa, canada);
        Page<Country> page = new PageImpl<>(countries);
        when(countryRepository.findAllByNameContainingIgnoreCase(eq(""), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Country> result = countryService.searchByName(null, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2)
                                     .containsExactly(usa, canada);
    }

    @Test
    void searchByName_ShouldReturnFilteredCountries_WhenQueryProvided() {
        // Arrange
        List<Country> countries = Arrays.asList(canada);
        Page<Country> page = new PageImpl<>(countries);
        when(countryRepository.findAllByNameContainingIgnoreCase(eq("Can"), any(Pageable.class)))
            .thenReturn(page);

        // Act
        Page<Country> result = countryService.searchByName("Can", PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1)
                                     .containsExactly(canada);
    }

    @Test
    void get_ShouldReturnCountry_WhenValidIdProvided() {
        // Arrange
        when(countryRepository.findById(1L)).thenReturn(Optional.of(usa));

        // Act
        Country result = countryService.get(1L);

        // Assert
        assertThat(result).isNotNull()
                         .isEqualTo(usa);
    }

    @Test
    void get_ShouldThrowException_WhenInvalidIdProvided() {
        // Arrange
        when(countryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> countryService.get(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Country not found");
    }

    @Test
    void create_ShouldSaveAndReturnCountry() {
        // Arrange
        Country newCountry = new Country(null, "JPN", "Japan");
        Country savedCountry = new Country(3L, "JPN", "Japan");
        when(countryRepository.save(newCountry)).thenReturn(savedCountry);

        // Act
        Country result = countryService.create(newCountry);

        // Assert
        assertThat(result).isNotNull()
                         .isEqualTo(savedCountry);
        verify(countryRepository).save(newCountry);
    }

    @Test
    void update_ShouldUpdateExistingCountry() {
        // Arrange
        Country update = new Country(1L, "USA", "United States of America");
        when(countryRepository.findById(1L)).thenReturn(Optional.of(usa));
        when(countryRepository.save(any(Country.class))).thenReturn(update);

        // Act
        Country result = countryService.update(1L, update);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("United States of America");
        verify(countryRepository).save(any(Country.class));
    }

    @Test
    void delete_ShouldDeleteCountry() {
        // Arrange
        doNothing().when(countryRepository).deleteById(1L);

        // Act
        countryService.delete(1L);

        // Assert
        verify(countryRepository).deleteById(1L);
    }

    @Test
    void findAll_ShouldReturnAllCountries() {
        // Arrange
        List<Country> countries = Arrays.asList(usa, canada);
        when(countryRepository.findAll()).thenReturn(countries);

        // Act
        List<Country> result = countryService.findAll();

        // Assert
        assertThat(result).hasSize(2)
                         .containsExactly(usa, canada);
    }
}