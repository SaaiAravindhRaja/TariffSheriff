package com.tariffsheriff.backend.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigGeneratedTest {

    private SecurityConfig securityConfig;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        mockRequest = new MockHttpServletRequest();
    }

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoder() {
        // Act
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        // Assert
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void corsConfigurationSource_parsesAllowedOrigins() {
        // Arrange
        String origins = "http://example.com, https://app.com";
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", origins);
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOriginPatterns", "");

        // Act
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(mockRequest);

        // Assert
        assertNotNull(config);
        assertEquals(List.of("http://example.com", "https://app.com"), config.getAllowedOrigins());
        assertNull(config.getAllowedOriginPatterns()); // It's null because the list was empty
        assertTrue(config.getAllowCredentials());
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), config.getAllowedMethods());
    }

    @Test
    void corsConfigurationSource_parsesAllowedOriginPatterns() {
        // Arrange
        String patterns = "http://localhost:*, http://*.example.com";
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOriginPatterns", patterns);

        // Act
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(mockRequest);

        // Assert
        assertNotNull(config);
        assertNull(config.getAllowedOrigins()); // It's null because the list was empty
        assertEquals(List.of("http://localhost:*", "http://*.example.com"), config.getAllowedOriginPatterns());
    }

    @Test
    void corsConfigurationSource_handlesNullAndBlankValues() {
        // Arrange
        String patterns = " , ,,   "; // Tests the parseList filter
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", null);
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOriginPatterns", patterns);

        // Act
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(mockRequest);

        // Assert
        assertNotNull(config);
        assertNull(config.getAllowedOrigins());
        assertNull(config.getAllowedOriginPatterns()); // The list is empty, so it's not set
    }
}