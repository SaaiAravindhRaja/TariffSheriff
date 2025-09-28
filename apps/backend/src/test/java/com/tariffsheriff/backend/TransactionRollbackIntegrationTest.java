package com.tariffsheriff.backend;

import com.tariffsheriff.backend.model.Country;
import com.tariffsheriff.backend.model.HsProduct;
import com.tariffsheriff.backend.service.CountryService;
import com.tariffsheriff.backend.service.HsProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class TransactionRollbackIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired CountryService countryService;
    @Autowired HsProductService hsProductService;

    @Test
    void write_fails_entirely_and_rolls_back() {
        // Create a country and remember HS count for destination
        Country destination = new Country();
        destination.setIso2("EE");
        destination.setIso3("EEE");
        destination.setName("Echo");
        destination = countryService.create(destination);

        long before = hsProductService.listByDestination(destination.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        // Try a multi-step write: create product ok, then force a failure by nulling required field during update
        final Country destForLambda = destination;
        assertThatThrownBy(() -> hsProductService.createThenFail(destForLambda))
            .isInstanceOf(RuntimeException.class);

        // After rollback, count should be unchanged
        long after = hsProductService.listByDestination(destination.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        assertThat(after).isEqualTo(before);
    }

    // helper removed; use service-level transactional method
}


