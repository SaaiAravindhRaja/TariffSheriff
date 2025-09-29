package com.tariffsheriff.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

// Helpful for loading .env files in local development 
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Load .env file from the current directory or classpath
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")  // Look for .env in current directory
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Convert dotenv entries to a Map and add as property source
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvMap.put(entry.getKey(), entry.getValue());
                System.out.println("DEBUG: Loaded env var: " + entry.getKey() + "=" + entry.getValue());
            });

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", dotenvMap));
            
            System.out.println("DEBUG: Successfully loaded .env file with " + dotenvMap.size() + " variables");
            System.out.println("DEBUG: DATABASE_URL = " + dotenvMap.get("DATABASE_URL"));
        } catch (Exception e) {
            // If .env file is missing or can't be loaded, continue without it
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}