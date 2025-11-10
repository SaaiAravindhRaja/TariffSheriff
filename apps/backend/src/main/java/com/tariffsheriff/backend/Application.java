package com.tariffsheriff.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties
@EntityScan(basePackages = {
        "com.tariffsheriff.backend.tariff.model",
        "com.tariffsheriff.backend.auth.entity",
        "com.tariffsheriff.backend.tariffcalculation.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.tariffsheriff.backend.tariff.repository",
        "com.tariffsheriff.backend.auth.repository",
        "com.tariffsheriff.backend.tariffcalculation.repository"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
