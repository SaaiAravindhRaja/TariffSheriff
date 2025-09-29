package com.tariffsheriff.backend.user.config;

import com.tariffsheriff.backend.security.JwtAuthenticationFilter;
import com.tariffsheriff.backend.security.JwtTokenProvider;
import com.tariffsheriff.backend.user.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

@Bean
public AuthenticationManager authManager(HttpSecurity http) throws Exception {
    return http.getSharedObject(AuthenticationManagerBuilder.class)
               .userDetailsService(userDetailsService)
               .passwordEncoder(passwordEncoder())
               .and()
               .build();
}

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            // Allow GET requests to /api/users/** for any authenticated user
            .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()
            // Restrict POST, PUT, DELETE to admins only
            .requestMatchers("/api/users/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
}


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
    // @Bean
    // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //     http
    //         .csrf(csrf -> csrf.disable()) // disable CSRF for curl testing
    //         .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    //     return http.build();
    // }
