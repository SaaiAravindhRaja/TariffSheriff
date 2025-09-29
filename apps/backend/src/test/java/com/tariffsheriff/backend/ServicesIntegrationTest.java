package com.tariffsheriff.backend;

import com.tariffsheriff.backend.model.*;
import com.tariffsheriff.backend.model.enums.AgreementStatus;
import com.tariffsheriff.backend.model.enums.TariffBasis;
import com.tariffsheriff.backend.model.enums.TariffRateType;
import com.tariffsheriff.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ServicesIntegrationTest {

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
    @Autowired TariffRateService tariffRateService;
    @Autowired VatService vatService;
    @Autowired AgreementService agreementService;
    @Autowired RooRuleService rooRuleService;

    @Test
    void country_crud_and_pagination() {
        Country c = new Country();
        c.setIso2("AA");
        c.setIso3("AAA");
        c.setName("Alpha");
        c = countryService.create(c);
        assertThat(c.getId()).isNotNull();

        Country got = countryService.get(c.getId());
        assertThat(got.getName()).isEqualTo("Alpha");

        got.setName("AlphaLand");
        Country updated = countryService.update(got.getId(), got);
        assertThat(updated.getName()).isEqualTo("AlphaLand");

        Page<Country> page = countryService.searchByName("alpha", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Country::getId).contains(c.getId());

        countryService.delete(c.getId());
        assertThat(countryService.searchByName("alpha", PageRequest.of(0, 10)).getContent())
            .extracting(Country::getId).doesNotContain(c.getId());
    }

    @Test
    void hsProduct_tariff_vat_and_agreement_roo_rules_flow() {
        // Countries
        Country importer = new Country(); importer.setIso2("BB"); importer.setIso3("BBB"); importer.setName("Beta");
        importer = countryService.create(importer);
        Country origin = new Country(); origin.setIso2("CC"); origin.setIso3("CCC"); origin.setName("Gamma");
        origin = countryService.create(origin);
        Country destination = new Country(); destination.setIso2("DD"); destination.setIso3("DDD"); destination.setName("Delta");
        destination = countryService.create(destination);

        // HS Product
        HsProduct product = new HsProduct();
        product.setDestination(destination);
        product.setHsVersion("2022");
        product.setHsCode("999999");
        product.setHsLabel("Demo product");
        product = hsProductService.create(product);
        assertThat(product.getId()).isNotNull();

        // VAT upsert/get
        vatService.upsert(importer.getId(), new BigDecimal("0.1234"));
        assertThat(vatService.getForImporter(importer.getId()).getStandardRate()).isEqualByComparingTo("0.1234");

        // Agreement with parties
        Agreement agreement = new Agreement();
        agreement.setName("Demo FTA");
        agreement.setType("FTA");
        agreement.setStatus(AgreementStatus.IN_FORCE);
        agreement.setEnteredIntoForce(LocalDate.now());
        agreement.setParties(Set.of(importer, origin));
        agreement = agreementService.create(agreement);
        assertThat(agreement.getId()).isNotNull();

        // Tariff rates: MFN and PREF
        TariffRate mfn = new TariffRate();
        mfn.setImporter(importer);
        mfn.setOrigin(null);
        mfn.setHsProduct(product);
        mfn.setBasis(TariffBasis.MFN);
        mfn.setAgreement(null);
        mfn.setRateType(TariffRateType.AD_VALOREM);
        mfn.setAdValoremRate(new BigDecimal("0.050000"));
        mfn.setValidFrom(LocalDate.of(2024,1,1));
        mfn = tariffRateService.create(mfn);

        TariffRate pref = new TariffRate();
        pref.setImporter(importer);
        pref.setOrigin(origin);
        pref.setHsProduct(product);
        pref.setBasis(TariffBasis.PREF);
        pref.setAgreement(agreement);
        pref.setRateType(TariffRateType.AD_VALOREM);
        pref.setAdValoremRate(new BigDecimal("0.010000"));
        pref.setValidFrom(LocalDate.of(2024,1,1));
        pref = tariffRateService.create(pref);

        // ROO rule for agreement/product
        RooRule rule = new RooRule();
        rule.setAgreement(agreement);
        rule.setHsProduct(product);
        rule.setMethod("CTC");
        rule.setThreshold("40%");
        rule.setRequiresCertificate(true);
        rule = rooRuleService.create(rule);
        assertThat(rule.getId()).isNotNull();

        // Lookup via service
        List<TariffRate> mfnFound = tariffRateService.findApplicableMfn(importer.getId(), product.getId(), LocalDate.of(2024,3,1));
        assertThat(mfnFound).isNotEmpty();
        assertThat(mfnFound.get(0).getAdValoremRate()).isEqualByComparingTo("0.050000");

        List<TariffRate> prefFound = tariffRateService.findApplicablePreferential(importer.getId(), origin.getId(), product.getId(), LocalDate.of(2024,3,1));
        assertThat(prefFound).isNotEmpty();
        assertThat(prefFound.get(0).getAdValoremRate()).isEqualByComparingTo("0.010000");

        // Update ROO rule
        rule.setThreshold("45%");
        RooRule updatedRule = rooRuleService.update(rule.getId(), rule);
        assertThat(updatedRule.getThreshold()).isEqualTo("45%");
    }
}


