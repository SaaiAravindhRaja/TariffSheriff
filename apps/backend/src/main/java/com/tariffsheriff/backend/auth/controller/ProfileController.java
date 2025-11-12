package com.tariffsheriff.backend.auth.controller;

import com.tariffsheriff.backend.auth.dto.UpdateProfileRequest;
import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.tariff.model.HsProduct;
import com.tariffsheriff.backend.tariff.repository.HsProductRepository;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;
import com.tariffsheriff.backend.tariffcalculation.repository.TariffCalculationRepository;
import com.tariffsheriff.backend.user.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final TariffCalculationRepository calculationRepository;
    private final TariffRateRepository tariffRateRepository;
    private final HsProductRepository hsProductRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("aboutMe", user.getAboutMe());

        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateProfileRequest request) {

        String email = jwt.getClaimAsString("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only allowed fields
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        user.setAboutMe(request.getAboutMe()); // Can be null or empty

        user = userRepository.save(user);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("aboutMe", user.getAboutMe());

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "today") String period) {

        String email = jwt.getClaimAsString("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();

        // 1. Total Tariff Revenue (sum of all user's calculations)
        BigDecimal totalRevenue = calculationRepository.sumTotalTariffByUserId(userId);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        // 2. Active Tariff Routes (distinct routes in database)
        Long activeTariffRoutes = tariffRateRepository.countDistinctTradeRoutes();

        // 3. Calculations count based on period
        LocalDateTime startDate = switch (period.toLowerCase()) {
            case "month" -> LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
            case "year" -> LocalDateTime.of(LocalDate.now().withDayOfYear(1), LocalTime.MIN);
            default -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN); // today
        };

        Long calculationsCount = calculationRepository.countByUserIdAndCreatedAtAfter(userId, startDate);

        // 4. Most Used HS Code
        DashboardStatsDto.MostUsedHsCodeDto mostUsedHsCode = null;
        List<Object[]> mostUsedResults = calculationRepository.findMostUsedHsCodeByUserId(userId, PageRequest.of(0, 1));

        if (!mostUsedResults.isEmpty()) {
            Object[] result = mostUsedResults.get(0);
            String hsCode = (String) result[0];
            Long count = (Long) result[1];

            // Get HS product description - handle case where HS code might not exist or
            // cause DB errors
            String description = "Unknown Product";
            try {
                description = hsProductRepository.findByHsCode(hsCode)
                        .map(HsProduct::getHsLabel)
                        .orElse("Unknown Product");
            } catch (Exception e) {
                // Log and continue with "Unknown Product" if lookup fails
                System.err.println("Failed to lookup HS code " + hsCode + ": " + e.getMessage());
            }

            mostUsedHsCode = new DashboardStatsDto.MostUsedHsCodeDto(hsCode, description, count);
        }

        DashboardStatsDto stats = new DashboardStatsDto(
                totalRevenue,
                activeTariffRoutes,
                calculationsCount,
                mostUsedHsCode);

        return ResponseEntity.ok(stats);
    }
}
