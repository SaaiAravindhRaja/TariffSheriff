package com.tariffsheriff.backend.config;

import com.tariffsheriff.backend.security.jwt.JwtAuthenticationFilter;
import com.tariffsheriff.backend.user.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive security configuration with JWT-based authentication.
 * Includes CORS configuration, security headers, CSRF protection, and role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * Configure password encoder with BCrypt.
     * Uses 12 rounds for strong security as per requirements.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configure authentication manager for Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure authentication provider with custom user details service.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configure CORS settings based on security properties.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from properties
        configuration.setAllowedOrigins(Arrays.asList(securityProperties.getAllowedOriginsArray()));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow common headers including authentication headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-CSRF-TOKEN"
        ));
        
        // Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        ));
        
        // Allow credentials for authentication
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Comprehensive security filter chain configuration with JWT authentication.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure CSRF protection
            .csrf(csrf -> csrf
                // Disable CSRF for stateless API endpoints
                .ignoringRequestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/tariff/**",
                    "/api/user/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                )
                // Enable CSRF for state-changing operations if needed
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            
            // Set session management to stateless for JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure authorization rules with role-based access control
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/verify", 
                               "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Authentication required endpoints
                .requestMatchers("/api/auth/logout", "/api/auth/refresh", "/api/auth/me").authenticated()
                .requestMatchers("/api/auth/change-password").authenticated()
                
                // User management endpoints - role-based access
                .requestMatchers("/api/user/profile").hasAnyRole("USER", "ANALYST", "ADMIN")
                .requestMatchers("/api/user/update-profile").hasAnyRole("USER", "ANALYST", "ADMIN")
                .requestMatchers("/api/user/list").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers("/api/user/*/role").hasRole("ADMIN")
                .requestMatchers("/api/user/*/status").hasRole("ADMIN")
                .requestMatchers("/api/user/admin/**").hasRole("ADMIN")
                
                // Tariff endpoints - role-based access
                .requestMatchers("/api/tariff/calculate").hasAnyRole("USER", "ANALYST", "ADMIN")
                .requestMatchers("/api/tariff/history").hasAnyRole("USER", "ANALYST", "ADMIN")
                .requestMatchers("/api/tariff/admin/**").hasAnyRole("ANALYST", "ADMIN")
                .requestMatchers("/api/tariff/bulk/**").hasRole("ADMIN")
                
                // Analytics endpoints - analyst and admin only
                .requestMatchers("/api/analytics/**").hasAnyRole("ANALYST", "ADMIN")
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            
            // Add comprehensive security headers
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions().deny()
                
                // Prevent MIME type sniffing
                .contentTypeOptions().and()
                
                // Enable XSS protection
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                    .preload(true)
                )
                
                // Add custom security headers
                .and()
                .headers(customHeaders -> customHeaders
                    .addHeaderWriter((request, response) -> {
                        // X-XSS-Protection header
                        response.setHeader("X-XSS-Protection", "1; mode=block");
                        
                        // Referrer Policy
                        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                        
                        // Permissions Policy (formerly Feature Policy)
                        response.setHeader("Permissions-Policy", 
                            "camera=(), microphone=(), geolocation=(), payment=()");
                        
                        // Content Security Policy
                        response.setHeader("Content-Security-Policy", 
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'");
                    })
                )
            );

        return http.build();
    }
}