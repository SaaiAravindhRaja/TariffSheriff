package com.tariffsheriff.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for TariffSheriff Authentication API
 * Requirement 12.1: OpenAPI/Swagger documentation for authentication endpoints
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "TariffSheriff Authentication API",
        version = "1.0.0",
        description = """
            # TariffSheriff Authentication API
            
            This API provides comprehensive user authentication and management functionality for the TariffSheriff platform.
            
            ## Features
            - User registration with email verification
            - Secure JWT-based authentication
            - Password reset and management
            - Role-based access control (USER, ANALYST, ADMIN)
            - Session management and token refresh
            - Comprehensive audit logging
            
            ## Authentication
            Most endpoints require authentication using JWT Bearer tokens. Include the token in the Authorization header:
            ```
            Authorization: Bearer <your-jwt-token>
            ```
            
            ## Rate Limiting
            Authentication endpoints are rate-limited to prevent abuse:
            - Login attempts: 5 per 15 minutes per IP
            - Registration: 3 per hour per IP
            - Password reset: 3 per hour per email
            
            ## Error Handling
            All endpoints return standardized error responses with appropriate HTTP status codes and detailed error messages.
            """,
        contact = @Contact(
            name = "TariffSheriff Support",
            email = "support@tariffsheriff.com",
            url = "https://tariffsheriff.com/support"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.tariffsheriff.com",
            description = "Production Server"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication. Obtain a token by calling the /api/auth/login endpoint."
)
public class OpenApiConfig {
}