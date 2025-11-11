package com.tariffsheriff.backend.tariffcalculation.controller;

import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariffcalculation.dto.*;
import com.tariffsheriff.backend.tariff.repository.AgreementRepository;
import com.tariffsheriff.backend.tariffcalculation.entity.TariffCalculation;
import com.tariffsheriff.backend.tariffcalculation.service.TariffCalculationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/tariff-calculations")
public class TariffCalculationController {

    private final TariffCalculationService service;
    private final UserRepository userRepository;
    private final AgreementRepository agreements;

    public TariffCalculationController(TariffCalculationService service,
                                       UserRepository userRepository,
                                       AgreementRepository agreements) {
        this.service = service;
        this.userRepository = userRepository;
        this.agreements = agreements;
    }

    private User requireUserFromJwt(Jwt jwt) {
        if (jwt == null) throw new AccessDeniedException("Unauthenticated");
        String email = getEmailFromJwtOrUserInfo(jwt);
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Email not available in token");
        }
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private String getEmailFromJwtOrUserInfo(Jwt jwt) {
        Object e = jwt.getClaims().get("email");
        if (e instanceof String s && !s.isBlank()) return s;
        Object e2 = jwt.getClaims().get("https://tariffsheriff.com/email");
        if (e2 instanceof String s2 && !s2.isBlank()) return s2;
        try {
            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            if (iss == null || iss.isBlank()) return null;
            String base = iss.endsWith("/") ? iss : iss + "/";
            URI uri = URI.create(base + "userinfo");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwt.getTokenValue());
            HttpEntity<Void> req = new HttpEntity<>(headers);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<Map> resp = rt.exchange(uri, HttpMethod.GET, req, Map.class);
            Object ev = resp.getBody() != null ? resp.getBody().get("email") : null;
            if (ev instanceof String s3 && !s3.isBlank()) return s3;
        } catch (Exception ignored) {}
        return null;
    }

    @PostMapping
    public TariffCalculationDetail save(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody SaveTariffCalculationRequest req) {
        User currentUser = requireUserFromJwt(jwt);
        TariffRateRequestDto input = req.getInput();
        TariffCalculation saved = service.saveForUser(
            currentUser,
            input,
            req.getResult(),
            req.getName(),
            req.getNotes(),
            req.getHsCode(),
            req.getImporterIso3(),
            req.getOriginIso3()
        );
        return toDetail(saved);
    }

    @GetMapping
    public Page<TariffCalculationSummary> list(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        User currentUser = requireUserFromJwt(jwt);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return service.listForUser(currentUser.getId(), pageable).map(this::toSummary);
    }

    @GetMapping("/{id}")
    public TariffCalculationDetail get(@PathVariable Long id,
                                       @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUserFromJwt(jwt);
        TariffCalculation tc = service.getForUser(id, currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Calculation not found"));
        return toDetail(tc);
    }

    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> delete(@PathVariable Long id,
                       @AuthenticationPrincipal Jwt jwt) {
        User currentUser = requireUserFromJwt(jwt);
        boolean deleted = service.deleteForUser(id, currentUser.getId());
        return deleted ? org.springframework.http.ResponseEntity.noContent().build()
                       : org.springframework.http.ResponseEntity.notFound().build();
    }

    private TariffCalculationSummary toSummary(TariffCalculation tc) {
        // Resolve agreement name (may be null)
        String agreementName = null;
        if (tc.getAgreementId() != null) {
            agreementName = agreements.findById(tc.getAgreementId())
                .map(a -> a.getName())
                .orElse(null);
        }
        // Compute total value fallback if missing (legacy rows): totalTariff = totalValue * appliedRate
        java.math.BigDecimal totalValue = tc.getTotalValue();
        if ((totalValue == null || totalValue.compareTo(java.math.BigDecimal.ZERO) == 0)
            && tc.getTotalTariff() != null
            && tc.getAppliedRate() != null
            && tc.getAppliedRate().compareTo(java.math.BigDecimal.ZERO) > 0) {
            totalValue = tc.getTotalTariff().divide(tc.getAppliedRate(), 2, java.math.RoundingMode.HALF_UP);
        }

        return new TariffCalculationSummary(
            tc.getId(),
            tc.getName(),
            tc.getCreatedAt(),
            tc.getTotalTariff(),
            totalValue,
            tc.getRateUsed(),
            tc.getAppliedRate(),
            tc.getRvcComputed(),
            tc.getRvc(),
            tc.getHsCode(),
            tc.getImporterIso3(),
            tc.getOriginIso3(),
            agreementName
        );
    }

    private TariffCalculationDetail toDetail(TariffCalculation tc) {
        TariffRateRequestDto input = new TariffRateRequestDto();
        input.setMfnRate(tc.getMfnRate());
        input.setPrefRate(tc.getPrefRate());
        input.setRvcThreshold(tc.getRvc());
        input.setAgreementId(tc.getAgreementId());
        input.setQuantity(tc.getQuantity());
        input.setTotalValue(tc.getTotalValue());
        input.setMaterialCost(tc.getMaterialCost());
        input.setLabourCost(tc.getLabourCost());
        input.setOverheadCost(tc.getOverheadCost());
        input.setProfit(tc.getProfit());
        input.setOtherCosts(tc.getOtherCosts());
        input.setFob(tc.getFob());
        input.setNonOriginValue(tc.getNonOriginValue());

        CalculationResult result = new CalculationResult(
            tc.getRvcComputed(),
            tc.getRateUsed(),
            tc.getAppliedRate(),
            tc.getTotalTariff()
        );

        return new TariffCalculationDetail(
            tc.getId(),
            input,
            result,
            tc.getName(),
            tc.getNotes(),
            tc.getHsCode(),
            tc.getImporterIso3(),
            tc.getOriginIso3(),
            tc.getCreatedAt()
        );
    }
}
