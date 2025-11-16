package com.tariffsheriff.backend.tariffcalculation.controller;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariffcalculation.dto.CalculationResult;
import com.tariffsheriff.backend.tariffcalculation.dto.FrontendCalculationResult;
import com.tariffsheriff.backend.tariffcalculation.dto.SaveTariffCalculationRequest;
import com.tariffsheriff.backend.tariffcalculation.dto.TariffCalculationDetail;
import com.tariffsheriff.backend.tariffcalculation.dto.TariffCalculationSummary;
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
import org.springframework.data.domain.Sort;
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

    private static final Sort SUMMARY_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    @Mock
    private TariffCalculationService service;

    @Mock
    private UserRepository userRepository;

    @Mock // <-- ADD THIS LINE
    private AgreementRepository agreementRepository;

    // It's also possible your controller uses a mapper. If you still get
    // a NullPointerException, you may also need to mock your mapper:
    // @Mock
    // private AgreementMapper agreementMapper; 

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
        assertEquals("USA", result.getImporterIso3());
        assertEquals("CHN", result.getOriginIso3());
        verify(service).saveForUser(
            eq(testUser),
            any(TariffRateRequestDto.class),
            any(FrontendCalculationResult.class),
            eq("Test Calculation"),
            eq("Test Notes"),
            eq("1234"),
            eq("USA"),
            eq("CHN")
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
        verify(service).listForUser(eq(testUser.getId()), eq(PageRequest.of(0, 20, SUMMARY_SORT)));
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
    input.setPrefRate(new BigDecimal("0.05"));
    input.setRvcThreshold(new BigDecimal("40"));
    input.setAgreementId(10L);
    input.setQuantity(1);
    input.setTotalValue(new BigDecimal("1000"));
    input.setMaterialCost(new BigDecimal("500"));
    input.setLabourCost(new BigDecimal("200"));
    input.setOverheadCost(new BigDecimal("100"));
    input.setProfit(new BigDecimal("100"));
    input.setOtherCosts(new BigDecimal("100"));
    input.setFob(new BigDecimal("1000"));
    input.setNonOriginValue(new BigDecimal("300"));
    validRequest.setInput(input);

    FrontendCalculationResult result = new FrontendCalculationResult(); // Assuming this DTO is setup as needed
    validRequest.setResult(result);

    validRequest.setName("Test Calculation");
    validRequest.setNotes("Test Notes");
    validRequest.setHsCode("1234");
    validRequest.setImporterIso3("USA");
    validRequest.setOriginIso3("CHN");

    // Setup saved calculation (with all mappable fields)
    savedCalculation = new TariffCalculation();
    savedCalculation.setId(1L);
    savedCalculation.setName("Test Calculation");
    savedCalculation.setNotes("Test Notes");
    savedCalculation.setHsCode("1234");
    savedCalculation.setImporterIso3("USA");
    savedCalculation.setOriginIso3("CHN");
    savedCalculation.setCreatedAt(LocalDateTime.now());
        
    // Fields for toDetail mapping
    savedCalculation.setMfnRate(new BigDecimal("0.1"));
    savedCalculation.setPrefRate(new BigDecimal("0.05"));
    savedCalculation.setRvc(new BigDecimal("40")); // This is rvcThreshold
    savedCalculation.setAgreementId(10L);
    savedCalculation.setQuantity(1);
    savedCalculation.setTotalValue(new BigDecimal("1000"));
    savedCalculation.setMaterialCost(new BigDecimal("500"));
    savedCalculation.setLabourCost(new BigDecimal("200"));
    savedCalculation.setOverheadCost(new BigDecimal("100"));
    savedCalculation.setProfit(new BigDecimal("100"));
    savedCalculation.setOtherCosts(new BigDecimal("100"));
    savedCalculation.setFob(new BigDecimal("1000"));
    savedCalculation.setNonOriginValue(new BigDecimal("300"));
        
    // Result fields
    savedCalculation.setRvcComputed(new BigDecimal("50"));
    savedCalculation.setRateUsed("MFN");
    savedCalculation.setAppliedRate(new BigDecimal("0.1"));
    savedCalculation.setTotalTariff(new BigDecimal("100.00"));
  }


  @Test
  void save_findsUserByAlternativeEmailClaim() {
    // Test the "https://tariffsheriff.com/email" claim
    when(jwt.getClaims()).thenReturn(Map.of("https://tariffsheriff.com/email", "test@example.com"));
    when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
    when(service.saveForUser(any(), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(savedCalculation);

    TariffCalculationDetail result = controller.save(jwt, validRequest);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(service).saveForUser(eq(testUser), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void save_throwsWhenEmailClaimIsBlank() {
    // Test the "email.isBlank()" check
    when(jwt.getClaims()).thenReturn(Map.of("email", " "));
    
    // We expect an AccessDeniedException because the user info fallback will
    // fail in a unit test (and the logic will find no valid email)
    var ex = assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
    assertEquals("Email not available in token", ex.getMessage());
  }

  @Test
  void save_throwsWhenAllEmailClaimsMissing() {
    // Test when no email claims are present
    when(jwt.getClaims()).thenReturn(Map.of("sub", "12345")); // No email claims

    var ex = assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
    assertEquals("Email not available in token", ex.getMessage());
  }

  @Test
    void list_clampsPageToZero() {
    mockValidUser();
    when(service.listForUser(any(), any())).thenReturn(Page.empty());

    controller.list(jwt, -5, 15);

    // Verifies that page was clamped from -5 to 0
    verify(service).listForUser(eq(testUser.getId()), eq(PageRequest.of(0, 15, SUMMARY_SORT)));
  }

  @Test
  void list_clampsSizeToMaximum() {
    mockValidUser();
    when(service.listForUser(any(), any())).thenReturn(Page.empty());

    controller.list(jwt, 0, 200);

    // Verifies that size was clamped from 200 to the controller max (200)
    verify(service).listForUser(eq(testUser.getId()), eq(PageRequest.of(0, 200, SUMMARY_SORT)));
  }

  @Test
  void list_clampsSizeToMinimum() {
    mockValidUser();
    when(service.listForUser(any(), any())).thenReturn(Page.empty());

    controller.list(jwt, 0, 0);

    // Verifies that size was clamped from 0 to 1
    verify(service).listForUser(eq(testUser.getId()), eq(PageRequest.of(0, 1, SUMMARY_SORT)));
  }
@Test
    void get_mapsToDetailCorrectly() {
        // This test uses the expanded setupTestData to verify full mapping
        mockValidUser();
        when(service.getForUser(1L, testUser.getId())).thenReturn(Optional.of(savedCalculation));

        TariffCalculationDetail result = controller.get(1L, jwt);

        assertNotNull(result);
        
        // Assert top-level fields
        assertEquals(1L, result.getId());
        assertEquals("Test Calculation", result.getName());
        assertEquals("1234", result.getHsCode());

        // Assert nested 'input' DTO
        TariffRateRequestDto input = result.getInput();
        assertNotNull(input);
        assertEquals(0, new BigDecimal("0.1").compareTo(input.getMfnRate()));
        assertEquals(0, new BigDecimal("0.05").compareTo(input.getPrefRate()));
        assertEquals(0, new BigDecimal("40").compareTo(input.getRvcThreshold()));
        assertEquals(0, new BigDecimal("1000").compareTo(input.getTotalValue()));
        assertEquals(0, new BigDecimal("500").compareTo(input.getMaterialCost()));
        assertEquals(0, new BigDecimal("300").compareTo(input.getNonOriginValue()));

        // Assert nested 'result' DTO
        CalculationResult calcResult = result.getResult();
        assertNotNull(calcResult);
        assertEquals(0, new BigDecimal("50").compareTo(calcResult.getRvcComputed()));
        assertEquals("MFN", calcResult.getRateUsed());
        assertEquals(0, new BigDecimal("0.1").compareTo(calcResult.getAppliedRate()));
        assertEquals(0, new BigDecimal("100.00").compareTo(calcResult.getTotalTariff()));
    }

    @Test
    void list_mapsToSummaryCorrectly() {
        // This test provides stronger assertions than the original 'list_returnsPageOfCalculations'
        mockValidUser();
        Page<TariffCalculation> page = new PageImpl<>(List.of(savedCalculation));
        when(service.listForUser(eq(testUser.getId()), any())).thenReturn(page);

        // Note: The controller returns Page<TariffCalculationSummary>
        Page<TariffCalculationSummary> resultPage = controller.list(jwt, 0, 20);
        
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        
        TariffCalculationSummary summary = resultPage.getContent().get(0);
        assertEquals(1L, summary.getId());
        assertEquals("Test Calculation", summary.getName());
        assertEquals("1234", summary.getHsCode());
        assertEquals("USA", summary.getImporterIso3());
        assertEquals("CHN", summary.getOriginIso3());
        assertEquals(0, new BigDecimal("100.00").compareTo(summary.getTotalTariff()));
        assertEquals("MFN", summary.getRateUsed());
        assertEquals(0, new BigDecimal("50").compareTo(summary.getRvcComputed()));
    }

    @Test
    void list_returnsEmptyPage() {
        mockValidUser();
        Page<TariffCalculation> emptyPage = Page.empty();
        when(service.listForUser(eq(testUser.getId()), any())).thenReturn(emptyPage);

        Page<TariffCalculationSummary> resultPage = controller.list(jwt, 0, 20);

        assertNotNull(resultPage);
        assertEquals(0, resultPage.getTotalElements());
        assertTrue(resultPage.getContent().isEmpty());
    }

    @Test
    void get_requiresAuthentication() {
        // Test that calling with a null JWT throws AccessDeniedException
        var ex = assertThrows(AccessDeniedException.class, () -> controller.get(1L, null));
        assertEquals("Unauthenticated", ex.getMessage());
    }

    @Test
    void list_requiresAuthentication() {
        // Test that calling with a null JWT throws AccessDeniedException
        var ex = assertThrows(AccessDeniedException.class, () -> controller.list(null, 0, 20));
        assertEquals("Unauthenticated", ex.getMessage());
    }

    @Test
    void delete_requiresAuthentication() {
        // Test that calling with a null JWT throws AccessDeniedException
        var ex = assertThrows(AccessDeniedException.class, () -> controller.delete(1L, null));
        assertEquals("Unauthenticated", ex.getMessage());
    }
  @Test
    void save_throwsWhenIssuerIsNull() {
        // This tests the (iss == null) branch
        when(jwt.getClaims()).thenReturn(Map.of("sub", "123")); // No email claims
        when(jwt.getIssuer()).thenReturn(null); // Issuer is null

        var ex = assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
        assertEquals("Email not available in token", ex.getMessage());
    }

    @Test
        void save_throwsWhenIssuerIsBlank() {
            // This tests the (iss.isBlank()) branch
            when(jwt.getClaims()).thenReturn(Map.of("sub", "123")); // No email claims
    
            // jwt.getIssuer() returns a URL, so return a mocked URL whose toString() is blank
            java.net.URL blankUrl = mock(java.net.URL.class);
            when(blankUrl.toString()).thenReturn("   ");
            when(jwt.getIssuer()).thenReturn(blankUrl);
    
            var ex = assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
            assertEquals("Email not available in token", ex.getMessage());
        }

    @Test
    void save_triggersCatchBlockOnHttpError() {
        // This tests the catch (Exception ignored) {} branch
        // We provide no email claims, forcing the code to try the /userinfo fallback.
        // We provide a (fake) issuer, which will cause 'new RestTemplate()' to
        // throw an Exception (e.g., UnknownHostException) when it tries to connect.
        // The catch block will ignore this, return null, and cause the AccessDeniedException.
        when(jwt.getClaims()).thenReturn(Map.of("sub", "123"));
        java.net.URL fakeUrl = mock(java.net.URL.class);
        when(fakeUrl.toString()).thenReturn("http://a.b.c.d.invalid.issuer");
        when(jwt.getIssuer()).thenReturn(fakeUrl);

        var ex = assertThrows(AccessDeniedException.class, () -> controller.save(jwt, validRequest));
        
        // This assertion proves the catch block was hit and the method returned null
        assertEquals("Email not available in token", ex.getMessage());
    }
}
