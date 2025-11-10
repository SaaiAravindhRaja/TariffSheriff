package com.tariffsheriff.backend.tariffcalculation.controller;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.dto.*;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import com.tariffsheriff.backend.tariffcalculation.service.TariffCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffCalculationControllerTest {

    @Mock
    TariffCalculationService service;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    TariffCalculationController controller;

    private Jwt jwtWithEmail(String email) {
        Map<String, Object> claims = Map.of("email", email);
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);
    }

    @Test
    void save_throwsWhenUnauthenticated() {
        SaveTariffCalculationRequest req = new SaveTariffCalculationRequest();
        assertThrows(AccessDeniedException.class, () -> controller.save(null, req));
    }

    @Test
    void save_callsService_andReturnsDetail_whenAuthenticated() {
        User u = new User();
        u.setId(3L);
        u.setEmail("me@example.com");

        Jwt jwt = jwtWithEmail("me@example.com");
        when(userRepository.findByEmailIgnoreCase("me@example.com")).thenReturn(Optional.of(u));

        // request
        TariffRateRequestDto input = new TariffRateRequestDto();
        input.setMfnRate(new BigDecimal("0.1"));
        SaveTariffCalculationRequest req = new SaveTariffCalculationRequest();
        req.setInput(input);
        FrontendCalculationResult res = new FrontendCalculationResult();
        res.setAppliedRate(new BigDecimal("0.1"));
        res.setCalculatedRvc(new BigDecimal("12"));
        res.setTariffBasis("MFN");
        res.setTotalCost(new BigDecimal("12.34"));
        req.setResult(res);
        req.setName("mycalc");

        TariffCalculation saved = new TariffCalculation();
        saved.setId(9L);
        saved.setName("mycalc");
        saved.setTotalTariff(new BigDecimal("12.34"));
        saved.setRateUsed("MFN");
        saved.setAppliedRate(new BigDecimal("0.1"));
        saved.setRvcComputed(new BigDecimal("12"));
        saved.setRvc(new BigDecimal("10"));
        saved.setHsCode("HS1");
        saved.setImporterIso2("GB");
        saved.setOriginIso2("CN");
        saved.setCreatedAt(LocalDateTime.now());

        when(service.saveForUser(u, input, res, "mycalc", null, null, null, null)).thenReturn(saved);

        TariffCalculationDetail detail = controller.save(jwt, req);
        assertNotNull(detail);
        assertEquals(9L, detail.getId());
        assertEquals("mycalc", detail.getName());
    assertEquals(new BigDecimal("12.34"), detail.getResult().getTotalTariff());
    }
}
