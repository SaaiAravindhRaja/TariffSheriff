package com.tariffsheriff.backend.tariffcalculation.controller;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.dto.CalculationResult;
import com.tariffsheriff.backend.tariffcalculation.dto.FrontendCalculationResult;
import com.tariffsheriff.backend.tariffcalculation.dto.SaveTariffCalculationRequest;
import com.tariffsheriff.backend.tariffcalculation.dto.TariffCalculationDetail;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import com.tariffsheriff.backend.tariffcalculation.service.TariffCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffCalculationControllerTest {

    @Mock
    private TariffCalculationService service;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TariffCalculationController controller;

    @Mock
    private Jwt jwt;

    private final User testUser = createTestUser();
    private SaveTariffCalculationRequest validRequest;
    private TariffCalculation savedCalculation;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void save_requiresAuthentication() {
        assertThrows(AccessDeniedException.class, () -> controller.save(null, validRequest));
    }

    @Test
    void save_throwsWhenUserNotFound() {
        when(jwt.getClaims()).thenReturn(Map.of("email", "unknown@example.com"));
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
    }

    @Test
    void save_savesAndReturnsCalculation() {
        mockValidUser();
        when(service.saveForUser(eq(testUser), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(savedCalculation);

        TariffCalculationDetail result = controller.save(jwt, validRequest);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Calculation", result.getName());
        assertEquals("1234", result.getHsCode());
        assertEquals("US", result.getImporterIso2());
        assertEquals("CN", result.getOriginIso2());
        verify(service).saveForUser(
            eq(testUser),
            any(TariffRateRequestDto.class),
            any(FrontendCalculationResult.class),
            eq("Test Calculation"),
            eq("Test Notes"),
            eq("1234"),
            eq("US"),
            eq("CN")
        );
    }

    @Test
    void list_returnsPageOfCalculations() {
        mockValidUser();
        Page<TariffCalculation> page = new PageImpl<>(List.of(savedCalculation));
        when(service.listForUser(eq(testUser.getId()), any())).thenReturn(page);

        Page<?> result = controller.list(jwt, 0, 20);
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(service).listForUser(eq(testUser.getId()), eq(PageRequest.of(0, 20)));
    }

    @Test
    void get_returnsCalculationWhenFound() {
        mockValidUser();
        when(service.getForUser(1L, testUser.getId())).thenReturn(Optional.of(savedCalculation));

        TariffCalculationDetail result = controller.get(1L, jwt);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Calculation", result.getName());
    }

    @Test
    void get_throwsWhenNotFound() {
        mockValidUser();
        when(service.getForUser(1L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.get(1L, jwt));
    }

    @Test
    void delete_callsService() {
        mockValidUser();
        controller.delete(1L, jwt);
        verify(service).deleteForUser(1L, testUser.getId());
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        return user;
    }

    private void mockValidUser() {
        when(jwt.getClaims()).thenReturn(Map.of("email", "test@example.com"));
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
    }

    private void setupTestData() {
        // Setup request
        validRequest = new SaveTariffCalculationRequest();
        TariffRateRequestDto input = new TariffRateRequestDto();
        input.setMfnRate(new BigDecimal("0.1"));
        input.setTotalValue(new BigDecimal("1000"));
        input.setMaterialCost(new BigDecimal("500"));
        input.setLabourCost(new BigDecimal("200"));
        input.setOverheadCost(new BigDecimal("100"));
        input.setProfit(new BigDecimal("100"));
        input.setOtherCosts(new BigDecimal("100"));
        input.setFob(new BigDecimal("1000"));
        validRequest.setInput(input);

        FrontendCalculationResult result = new FrontendCalculationResult();
        validRequest.setResult(result);

        validRequest.setName("Test Calculation");
        validRequest.setNotes("Test Notes");
        validRequest.setHsCode("1234");
        validRequest.setImporterIso2("US");
        validRequest.setOriginIso2("CN");

        // Setup saved calculation
        savedCalculation = new TariffCalculation();
        savedCalculation.setId(1L);
        savedCalculation.setName("Test Calculation");
        savedCalculation.setNotes("Test Notes");
        savedCalculation.setHsCode("1234");
        savedCalculation.setImporterIso2("US");
        savedCalculation.setOriginIso2("CN");
        savedCalculation.setTotalTariff(new BigDecimal("100.00"));
        savedCalculation.setRateUsed("MFN");
        savedCalculation.setAppliedRate(new BigDecimal("0.1"));
        savedCalculation.setRvcComputed(new BigDecimal("50"));
        savedCalculation.setRvc(new BigDecimal("40"));
        savedCalculation.setCreatedAt(LocalDateTime.now());
    }
}