package com.tariffsheriff.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that hits the Neon dev database using env vars:
 *   DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD
 * Run with profile dev:
 *   mvn -Dspring.profiles.active=dev -Dtest=NeonDevRetrievalIT test
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class NeonDevRetrievalIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void retrievesSeedMockDataFromNeon() {
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
        assertThat(productId).isNotNull();

        BigDecimal mfnRate = jdbcTemplate.queryForObject(
            "SELECT ad_valorem_rate FROM tariff_rate WHERE importer_id = ? AND hs_product_id = ? " +
                "AND basis = 'MFN' AND origin_id IS NULL AND valid_from <= ? " +
                "AND (valid_to IS NULL OR valid_to >= ?) ORDER BY valid_from DESC LIMIT 1",
            BigDecimal.class,
            1, productId, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1)
        );
        assertThat(mfnRate).isNotNull();

        BigDecimal vatRate = jdbcTemplate.queryForObject(
            "SELECT standard_rate FROM vat WHERE importer_id = ?",
            BigDecimal.class,
            1
        );
        assertThat(vatRate).isNotNull();
    }
}


