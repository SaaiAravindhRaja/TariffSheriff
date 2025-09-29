package com.tariffsheriff.backend;

import com.tariffsheriff.backend.model.HsProduct;
import com.tariffsheriff.backend.model.TariffRate;
import com.tariffsheriff.backend.model.Vat;
import com.tariffsheriff.backend.model.enums.TariffBasis;
import com.tariffsheriff.backend.repository.HsProductRepository;
import com.tariffsheriff.backend.repository.TariffRateRepository;
import com.tariffsheriff.backend.repository.VatRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class RepositoryQueryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired HsProductRepository hsProductRepository;
    @Autowired TariffRateRepository tariffRateRepository;
    @Autowired VatRepository vatRepository;

    @Test
    void hsProductLookup_byDestinationVersionCode_returnsExpectedProduct() {
        Optional<HsProduct> product = hsProductRepository
            .findByDestination_IdAndHsVersionAndHsCode(1L, "2022", "870380");
        assertThat(product).isPresent();
        assertThat(product.get().getId()).isEqualTo(1L);
    }

    @Test
    void tariffLookup_returnsApplicableMfnAndPreferential() {
        LocalDate date = LocalDate.of(2024, 3, 1);

        // MFN: importer 1, hs_product 1
        List<TariffRate> mfn = tariffRateRepository.findApplicableMfn(1L, 1L, date);
        assertThat(mfn).isNotEmpty();
        TariffRate topMfn = mfn.get(0);
        assertThat(topMfn.getBasis()).isEqualTo(TariffBasis.MFN);
        assertThat(topMfn.getAdValoremRate()).isEqualByComparingTo(new BigDecimal("0.1000"));

        // Preferential: importer 1, origin 2, hs_product 1
        List<TariffRate> pref = tariffRateRepository.findApplicablePreferential(1L, 2L, 1L, date);
        assertThat(pref).isNotEmpty();
        TariffRate topPref = pref.get(0);
        assertThat(topPref.getBasis()).isEqualTo(TariffBasis.PREF);
        assertThat(topPref.getAdValoremRate()).isEqualByComparingTo(new BigDecimal("0.0000"));
    }

    @Test
    void vatLookup_byImporter_returnsRate() {
        Optional<Vat> vat = vatRepository.findByImporter_Id(1L);
        assertThat(vat).isPresent();
        assertThat(vat.get().getStandardRate()).isEqualByComparingTo("0.2000");
    }
}


