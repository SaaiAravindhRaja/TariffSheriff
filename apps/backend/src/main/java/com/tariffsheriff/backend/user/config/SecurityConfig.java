package com.tariffsheriff.backend.user.config;

import com.tariffsheriff.backend.user.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Basic security configuration: secure /api/users/** endpoints, others are public
    // @Bean
    // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //     http
    //         .csrf(csrf -> csrf.disable()) // disable CSRF for curl testing
    //         .authorizeHttpRequests(auth -> auth
    //             .requestMatchers("/api/users/**").authenticated()
    //             .anyRequest().permitAll()
    //         )
    //         .httpBasic(org.springframework.security.config.Customizer.withDefaults()); // use basic auth
    //     return http.build();
    // }

    //The following method is to disable security for all endpoints (not recommended for production)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}