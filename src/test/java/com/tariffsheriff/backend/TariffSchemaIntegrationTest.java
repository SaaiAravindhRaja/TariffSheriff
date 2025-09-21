package com.tariffsheriff.backend;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TariffSchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migrationsProvideExpectedMockData() {
        Integer migrationsApplied = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Integer.class
        );
        assertThat(migrationsApplied).isNotNull().isGreaterThanOrEqualTo(3);

        Integer productId = jdbcTemplate.queryForObject(
            "SELECT id FROM hs_product WHERE destination_id = ? AND hs_version = ? AND hs_code = ?",
            Integer.class,
            1, "2022", "870380"
        );
        assertThat(productId).isEqualTo(1);

        BigDecimal mfnRate = jdbcTemplate.queryForObject(
            "SELECT ad_valorem_rate FROM tariff_rate WHERE importer_id = ? AND hs_product_id = ? " +
                "AND basis = 'MFN' AND origin_id IS NULL AND valid_from <= ? " +
                "AND (valid_to IS NULL OR valid_to >= ?) ORDER BY valid_from DESC LIMIT 1",
            BigDecimal.class,
            1, productId, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1)
        );
        assertThat(mfnRate).isNotNull().isEqualByComparingTo("0.1000");

        BigDecimal prefRate = jdbcTemplate.queryForObject(
            "SELECT ad_valorem_rate FROM tariff_rate WHERE importer_id = ? AND origin_id = ? AND hs_product_id = ? " +
                "AND basis = 'PREF' AND valid_from <= ? " +
                "AND (valid_to IS NULL OR valid_to >= ?) ORDER BY valid_from DESC LIMIT 1",
            BigDecimal.class,
            1, 2, productId, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1)
        );
        assertThat(prefRate).isNotNull().isEqualByComparingTo("0.0000");

        BigDecimal vatRate = jdbcTemplate.queryForObject(
            "SELECT standard_rate FROM vat WHERE importer_id = ?",
            BigDecimal.class,
            1
        );
        assertThat(vatRate).isNotNull().isEqualByComparingTo("0.2000");
    }
}
