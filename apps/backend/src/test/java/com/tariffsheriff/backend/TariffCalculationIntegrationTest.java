package com.tariffsheriff.backend;

import com.tariffsheriff.backend.model.TariffRate;
import com.tariffsheriff.backend.service.TariffRateService;
import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.service.TariffCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TariffCalculationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired TariffCalculationService calculationService;
    @Autowired TariffRateService tariffRateService;

    @Test
    void calculate_tariff_uses_neon_data_and_prints_output() {
        // Retrieve existing mock data from DB (seeded by migrations): importer=1, origin=2, hs_product=1
        List<TariffRate> mfn = tariffRateService.findApplicableMfn(1L, 1L, LocalDate.of(2024, 3, 1));
        List<TariffRate> pref = tariffRateService.findApplicablePreferential(1L, 2L, 1L, LocalDate.of(2024, 3, 1));
        assertThat(mfn).isNotEmpty();
        assertThat(pref).isNotEmpty();

        TariffRateRequestDto rq = new TariffRateRequestDto();
        rq.setImporter_id(java.math.BigInteger.valueOf(1));
        rq.setOrigin_id(java.math.BigInteger.valueOf(2));
        rq.setHsCode(1L);
        rq.setQuantity(100);
        rq.setTotalValue(new BigDecimal("1000.00"));
        rq.setMaterialCost(new BigDecimal("300.00"));
        rq.setLabourCost(new BigDecimal("200.00"));
        rq.setOverheadCost(new BigDecimal("50.00"));
        rq.setProfit(new BigDecimal("50.00"));
        rq.setOtherCosts(new BigDecimal("0.00"));
        rq.setFOB(new BigDecimal("1000.00"));
        rq.setNonOriginValue(new BigDecimal("400.00"));

        BigDecimal result = calculationService.calculateTariffRate(rq);
        // Compute RVC used and resolve threshold from preferential agreement for reporting
        BigDecimal rvc = rq.getMaterialCost()
            .add(rq.getLabourCost())
            .add(rq.getOverheadCost())
            .add(rq.getProfit())
            .add(rq.getOtherCosts())
            .divide(rq.getFOB(), 6, RoundingMode.HALF_UP);

        TariffRate mfnTop = mfn.get(0);
        TariffRate prefTop = pref.get(0);
        BigDecimal rvcThresholdPercent = prefTop.getAgreement() != null ? prefTop.getAgreement().getRvcThreshold() : null;
        BigDecimal rvcThresholdRatio = rvcThresholdPercent != null ? rvcThresholdPercent.movePointLeft(2) : null;

        boolean prefApplied = rvcThresholdRatio != null && rvc.compareTo(rvcThresholdRatio) >= 0;
        TariffRate applied = prefApplied ? prefTop : mfnTop;

        System.out.println("--- Tariff Calculation Inputs ---");
        System.out.println("importer_id=" + rq.getImporter_id() + ", origin_id=" + rq.getOrigin_id() + ", hs_product_id=" + rq.getHsCode());
        System.out.println("total_value=" + rq.getTotalValue().toPlainString() + ", quantity=" + rq.getQuantity());
        System.out.println("material=" + rq.getMaterialCost().toPlainString() + ", labour=" + rq.getLabourCost().toPlainString() + ", overhead=" + rq.getOverheadCost().toPlainString() + ", profit=" + rq.getProfit().toPlainString() + ", other=" + rq.getOtherCosts().toPlainString());
        System.out.println("FOB=" + rq.getFOB().toPlainString() + ", RVC=" + rvc.setScale(6, RoundingMode.HALF_UP).toPlainString());
        System.out.println("rvc_threshold_percent=" + (rvcThresholdPercent != null ? rvcThresholdPercent.toPlainString() : "<none>") +
            ", rvc_threshold_ratio=" + (rvcThresholdRatio != null ? rvcThresholdRatio.toPlainString() : "<none>"));

        System.out.println("--- Tariff Rates (top applicable by date) ---");
        System.out.println("MFN: basis=" + mfnTop.getBasis() + ", type=" + mfnTop.getRateType() + ", ad_valorem=" + (mfnTop.getAdValoremRate() != null ? mfnTop.getAdValoremRate().toPlainString() : "<null>") +
            ", specific_amount=" + (mfnTop.getSpecificAmount() != null ? mfnTop.getSpecificAmount().toPlainString() : "<null>") + ", specific_unit=" + (mfnTop.getSpecificUnit() != null ? mfnTop.getSpecificUnit() : "<null>"));
        System.out.println("PREF: basis=" + prefTop.getBasis() + ", type=" + prefTop.getRateType() + ", ad_valorem=" + (prefTop.getAdValoremRate() != null ? prefTop.getAdValoremRate().toPlainString() : "<null>") +
            ", specific_amount=" + (prefTop.getSpecificAmount() != null ? prefTop.getSpecificAmount().toPlainString() : "<null>") + ", specific_unit=" + (prefTop.getSpecificUnit() != null ? prefTop.getSpecificUnit() : "<null>") +
            ", agreement_rvc_threshold=" + (rvcThresholdPercent != null ? rvcThresholdPercent.toPlainString() : "<none>") + ")");

        System.out.println("--- Applied ---");
        System.out.println((prefApplied ? "Preferential" : "MFN") + " applied. Calculated tariff: " + result.setScale(2, RoundingMode.HALF_UP).toPlainString());

        assertThat(result).isNotNull();
    }
}


