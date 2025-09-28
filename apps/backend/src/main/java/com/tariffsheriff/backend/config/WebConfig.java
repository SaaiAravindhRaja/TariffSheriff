package com.tariffsheriff.backend.config;

import com.tariffsheriff.backend.security.ratelimit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering interceptors and other web-related settings.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply rate limiting to authentication endpoints
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                    "/api/auth/login",
                    "/api/auth/register", 
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password"
                )
                .order(1); // Execute before other interceptors
    }
}