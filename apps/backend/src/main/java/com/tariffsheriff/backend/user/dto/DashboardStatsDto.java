package com.tariffsheriff.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private BigDecimal totalTariffRevenue;
    private Long activeTariffRoutes;
    private Long calculationsCount;
    private MostUsedHsCodeDto mostUsedHsCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MostUsedHsCodeDto {
        private String hsCode;
        private String description;
        private Long count;
    }
}
