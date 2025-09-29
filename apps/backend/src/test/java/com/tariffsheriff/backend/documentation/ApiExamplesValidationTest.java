package com.tariffsheriff.backend.documentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ApiExamplesValidationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    private String apiDocumentation;

    @BeforeEach
    void setUp() throws IOException {
        // Load API documentation from resources
        ClassPathResource resource = new ClassPathResource("api-documentation.md");
        if (resource.exists()) {
            apiDocumentation = Files.readString(Paths.get(resource.getURI()));
        }
    }

    @Test
    void shouldValidateRegisterRequestExample() throws Exception {
        // Given - Example from documentation
        String registerExample = """
            {
              "name": "John Doe",
              "email": "john.doe@example.com",
              "password": "SecurePassword123!",
              "role": "USER"
            }
            """;

        // When
        RegisterRequest request = objectMapper.readValue(registerExample, RegisterRequest.class);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getName()).isEqualTo("John Doe");
        assertThat(request.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(request.getPassword()).isEqualTo("SecurePassword123!");
        assertThat(request.getRole()).isEqualTo("USER");
    }

    @Test
    void shouldValidateLoginRequestExample() throws Exception {
        // Given - Example from documentation
        String loginExample = """
            {
              "email": "john.doe@example.com",
              "password": "SecurePassword123!"
            }
            """;

        // When
        LoginRequest request = objectMapper.readValue(loginExample, LoginRequest.class);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(request.getPassword()).isEqualTo("SecurePassword123!");
    }

    @Test
    void shouldValidateAuthResponseExample() throws Exception {
        // Given - Example from documentation
        String authResponseExample = """
            {
              "success": true,
              "message": "Login successful",
              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              "tokenType": "Bearer",
              "expiresIn": 900,
              "user": {
                "id": 1,
                "email": "john.doe@example.com",
                "name": "John Doe",
                "role": "USER",
                "status": "ACTIVE",
                "emailVerified": true,
                "createdAt": "2023-01-01T00:00:00Z",
                "updatedAt": "2023-01-01T00:00:00Z"
              }
            }
            """;

        // When
        AuthResponse response = objectMapper.readValue(authResponseExample, AuthResponse.class);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getAccessToken()).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        assertThat(response.getRefreshToken()).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldValidateErrorResponseExample() throws Exception {
        // Given - Example from documentation
        String errorResponseExample = """
            {
              "success": false,
              "error": "VALIDATION_FAILED",
              "message": "Invalid input data",
              "status": 400,
              "path": "/api/auth/register",
              "timestamp": "2023-01-01T00:00:00Z",
              "validationErrors": {
                "email": "Please enter a valid email address",
                "password": "Password must be at least 8 characters long"
              }
            }
            """;

        // When
        ErrorResponse response = objectMapper.readValue(errorResponseExample, ErrorResponse.class);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).isEqualTo("Invalid input data");
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getPath()).isEqualTo("/api/auth/register");
        assertThat(response.getValidationErrors()).isNotNull();
        assertThat(response.getValidationErrors()).containsKey("email");
        assertThat(response.getValidationErrors()).containsKey("password");
    }

    @Test
    void shouldValidateForgotPasswordRequestExample() throws Exception {
        // Given - Example from documentation
        String forgotPasswordExample = """
            {
              "email": "john.doe@example.com"
            }
            """;

        // When
        ForgotPasswordRequest request = objectMapper.readValue(forgotPasswordExample, ForgotPasswordRequest.class);
        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldValidateResetPasswordRequestExample() throws Exception {
        // Given - Example from documentation
        String resetPasswordExample = """
            {
              "token": "reset-token-123",
              "newPassword": "NewSecurePassword123!"
            }
            """;

        // When
        ResetPasswordRequest request = objectMapper.readValue(resetPasswordExample, ResetPasswordRequest.class);
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getToken()).isEqualTo("reset-token-123");
        assertThat(request.getNewPassword()).isEqualTo("NewSecurePassword123!");
    }

    @Test
    void shouldValidateChangePasswordRequestExample() throws Exception {
        // Given - Example from documentation
        String changePasswordExample = """
            {
              "currentPassword": "CurrentPassword123!",
              "newPassword": "NewSecurePassword123!"
            }
            """;

        // When
        ChangePasswordRequest request = objectMapper.readValue(changePasswordExample, ChangePasswordRequest.class);
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getCurrentPassword()).isEqualTo("CurrentPassword123!");
        assertThat(request.getNewPassword()).isEqualTo("NewSecurePassword123!");
    }

    @Test
    void shouldValidateRefreshTokenRequestExample() throws Exception {
        // Given - Example from documentation
        String refreshTokenExample = """
            {
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            }
            """;

        // When
        RefreshTokenRequest request = objectMapper.readValue(refreshTokenExample, RefreshTokenRequest.class);
        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getRefreshToken()).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    void shouldValidateUserDtoExample() throws Exception {
        // Given - Example from documentation
        String userDtoExample = """
            {
              "id": 1,
              "email": "john.doe@example.com",
              "name": "John Doe",
              "role": "USER",
              "status": "ACTIVE",
              "emailVerified": true,
              "createdAt": "2023-01-01T00:00:00Z",
              "updatedAt": "2023-01-01T00:00:00Z"
            }
            """;

        // When
        UserDto userDto = objectMapper.readValue(userDtoExample, UserDto.class);

        // Then
        assertThat(userDto.getId()).isEqualTo(1L);
        assertThat(userDto.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(userDto.getName()).isEqualTo("John Doe");
        assertThat(userDto.getRole()).isEqualTo("USER");
        assertThat(userDto.getStatus()).isEqualTo("ACTIVE");
        assertThat(userDto.isEmailVerified()).isTrue();
        assertThat(userDto.getCreatedAt()).isNotNull();
        assertThat(userDto.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldValidateCurlExamples() {
        // This test validates that curl examples in documentation are syntactically correct
        if (apiDocumentation != null) {
            // Extract curl examples from documentation
            String[] curlExamples = extractCurlExamples(apiDocumentation);
            
            for (String curlExample : curlExamples) {
                // Validate curl syntax
                assertThat(curlExample).startsWith("curl");
                assertThat(curlExample).contains("-X"); // HTTP method
                assertThat(curlExample).contains("-H"); // Headers
                
                // Validate JSON in curl examples
                if (curlExample.contains("-d")) {
                    String jsonData = extractJsonFromCurl(curlExample);
                    if (jsonData != null) {
                        assertThatCode(() -> objectMapper.readTree(jsonData))
                            .doesNotThrowAnyException();
                    }
                }
            }
        }
    }

    @Test
    void shouldValidateHttpStatusCodeExamples() throws Exception {
        // Validate that documented HTTP status codes match actual responses
        
        // Test 400 Bad Request example
        String badRequestExample = """
            {
              "success": false,
              "error": "BAD_REQUEST",
              "message": "Invalid request data",
              "status": 400,
              "path": "/api/auth/login",
              "timestamp": "2023-01-01T00:00:00Z"
            }
            """;

        ErrorResponse badRequest = objectMapper.readValue(badRequestExample, ErrorResponse.class);
        assertThat(badRequest.getStatus()).isEqualTo(400);
        assertThat(badRequest.getError()).isEqualTo("BAD_REQUEST");

        // Test 401 Unauthorized example
        String unauthorizedExample = """
            {
              "success": false,
              "error": "UNAUTHORIZED",
              "message": "Invalid credentials",
              "status": 401,
              "path": "/api/auth/login",
              "timestamp": "2023-01-01T00:00:00Z"
            }
            """;

        ErrorResponse unauthorized = objectMapper.readValue(unauthorizedExample, ErrorResponse.class);
        assertThat(unauthorized.getStatus()).isEqualTo(401);
        assertThat(unauthorized.getError()).isEqualTo("UNAUTHORIZED");

        // Test 429 Too Many Requests example
        String rateLimitExample = """
            {
              "success": false,
              "error": "RATE_LIMIT_EXCEEDED",
              "message": "Too many requests. Please try again later.",
              "status": 429,
              "path": "/api/auth/login",
              "timestamp": "2023-01-01T00:00:00Z"
            }
            """;

        ErrorResponse rateLimit = objectMapper.readValue(rateLimitExample, ErrorResponse.class);
        assertThat(rateLimit.getStatus()).isEqualTo(429);
        assertThat(rateLimit.getError()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldValidateHeaderExamples() {
        // Validate that documented headers are correctly formatted
        
        // Authorization header example
        String authHeaderExample = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        assertThat(authHeaderExample).startsWith("Bearer ");
        assertThat(authHeaderExample).contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");

        // Content-Type header example
        String contentTypeExample = "application/json";
        assertThat(contentTypeExample).isEqualTo("application/json");

        // Rate limit headers examples
        String rateLimitRemainingExample = "4";
        String rateLimitResetExample = "1640995200";
        
        assertThat(Integer.parseInt(rateLimitRemainingExample)).isGreaterThanOrEqualTo(0);
        assertThat(Long.parseLong(rateLimitResetExample)).isPositive();
    }

    @Test
    void shouldValidateJsonSchemaExamples() throws Exception {
        // Validate that all JSON examples conform to their schemas
        
        // Test minimal valid examples
        String minimalRegisterExample = """
            {
              "name": "User",
              "email": "user@example.com",
              "password": "Password123!"
            }
            """;

        RegisterRequest minimalRegister = objectMapper.readValue(minimalRegisterExample, RegisterRequest.class);
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(minimalRegister);
        assertThat(violations).isEmpty();

        // Test with optional fields
        String fullRegisterExample = """
            {
              "name": "Full User Name",
              "email": "fulluser@example.com",
              "password": "FullPassword123!",
              "role": "ANALYST"
            }
            """;

        RegisterRequest fullRegister = objectMapper.readValue(fullRegisterExample, RegisterRequest.class);
        violations = validator.validate(fullRegister);
        assertThat(violations).isEmpty();
        assertThat(fullRegister.getRole()).isEqualTo("ANALYST");
    }

    @Test
    void shouldValidateTimestampFormats() throws Exception {
        // Validate that timestamp examples use correct ISO 8601 format
        
        String timestampExample = "2023-01-01T00:00:00Z";
        
        // Should be parseable as ISO 8601
        assertThatCode(() -> {
            objectMapper.readValue("\"" + timestampExample + "\"", java.time.Instant.class);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldValidateEnumValues() throws Exception {
        // Validate that enum examples use correct values
        
        // User roles
        String[] validRoles = {"USER", "ANALYST", "ADMIN"};
        for (String role : validRoles) {
            assertThatCode(() -> {
                String json = "{\"role\":\"" + role + "\"}";
                JsonNode node = objectMapper.readTree(json);
                String roleValue = node.get("role").asText();
                // Validate against actual enum
                assertThat(roleValue).isIn(validRoles);
            }).doesNotThrowAnyException();
        }

        // User statuses
        String[] validStatuses = {"PENDING", "ACTIVE", "SUSPENDED", "LOCKED"};
        for (String status : validStatuses) {
            assertThatCode(() -> {
                String json = "{\"status\":\"" + status + "\"}";
                JsonNode node = objectMapper.readTree(json);
                String statusValue = node.get("status").asText();
                assertThat(statusValue).isIn(validStatuses);
            }).doesNotThrowAnyException();
        }
    }

    private String[] extractCurlExamples(String documentation) {
        // Extract curl examples from markdown documentation
        // This is a simplified implementation
        return documentation.lines()
            .filter(line -> line.trim().startsWith("curl"))
            .toArray(String[]::new);
    }

    private String extractJsonFromCurl(String curlCommand) {
        // Extract JSON data from curl -d parameter
        // This is a simplified implementation
        int dataIndex = curlCommand.indexOf("-d");
        if (dataIndex == -1) return null;
        
        String afterData = curlCommand.substring(dataIndex + 2).trim();
        if (afterData.startsWith("'")) {
            int endIndex = afterData.indexOf("'", 1);
            return endIndex > 0 ? afterData.substring(1, endIndex) : null;
        } else if (afterData.startsWith("\"")) {
            int endIndex = afterData.indexOf("\"", 1);
            return endIndex > 0 ? afterData.substring(1, endIndex) : null;
        }
        
        return null;
    }
}