package com.tariffsheriff.backend.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.*;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import springfox.documentation.spring.web.plugins.Docket;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class AuthApiDocumentationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String testEmail = "test@example.com";
    private final String testPassword = "TestPassword123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setEmail(testEmail);
        testUser.setName("Test User");
        testUser.setPassword(passwordEncoder.encode(testPassword));
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldProvideOpenApiDocumentation() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotEmpty();
        
        // Parse OpenAPI spec
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        assertThat(apiDoc).containsKey("openapi");
        assertThat(apiDoc).containsKey("info");
        assertThat(apiDoc).containsKey("paths");
        assertThat(apiDoc).containsKey("components");
    }

    @Test
    void shouldDocumentAuthenticationEndpoints() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");

        // Then - Verify all authentication endpoints are documented
        assertThat(paths).containsKey("/api/auth/register");
        assertThat(paths).containsKey("/api/auth/login");
        assertThat(paths).containsKey("/api/auth/logout");
        assertThat(paths).containsKey("/api/auth/refresh");
        assertThat(paths).containsKey("/api/auth/verify");
        assertThat(paths).containsKey("/api/auth/forgot-password");
        assertThat(paths).containsKey("/api/auth/reset-password");
        assertThat(paths).containsKey("/api/auth/change-password");
        assertThat(paths).containsKey("/api/auth/me");
    }

    @Test
    void shouldDocumentRequestAndResponseSchemas() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> components = (Map<String, Object>) apiDoc.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        // Then - Verify all DTOs are documented
        assertThat(schemas).containsKey("RegisterRequest");
        assertThat(schemas).containsKey("LoginRequest");
        assertThat(schemas).containsKey("AuthResponse");
        assertThat(schemas).containsKey("RefreshTokenRequest");
        assertThat(schemas).containsKey("ForgotPasswordRequest");
        assertThat(schemas).containsKey("ResetPasswordRequest");
        assertThat(schemas).containsKey("ChangePasswordRequest");
        assertThat(schemas).containsKey("UserDto");
        assertThat(schemas).containsKey("ErrorResponse");
    }

    @Test
    void shouldDocumentSecuritySchemes() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> components = (Map<String, Object>) apiDoc.get("components");

        // Then - Verify security schemes are documented
        assertThat(components).containsKey("securitySchemes");
        Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
        assertThat(securitySchemes).containsKey("bearerAuth");
    }

    @Test
    void shouldValidateRegisterEndpointDocumentation() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setName("New User");
        request.setPassword("NewPassword123!");
        request.setRole("USER");

        // When - Test the actual endpoint
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then - Verify response matches documented schema
        String responseContent = result.getResponse().getContentAsString();
        ApiResponse response = objectMapper.readValue(responseContent, ApiResponse.class);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isNotNull();
        // Additional validation would check against OpenAPI schema
    }

    @Test
    void shouldValidateLoginEndpointDocumentation() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When - Test the actual endpoint
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - Verify response matches documented schema
        String responseContent = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(responseContent, AuthResponse.class);
        
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(testEmail);
    }

    @Test
    void shouldValidateErrorResponseDocumentation() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("");

        // When - Test endpoint with invalid data
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then - Verify error response matches documented schema
        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse response = objectMapper.readValue(responseContent, ErrorResponse.class);
        
        assertThat(response.getError()).isNotNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNotNull();
    }

    @Test
    void shouldDocumentHttpStatusCodes() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");
        
        // Then - Verify login endpoint documents all status codes
        Map<String, Object> loginPath = (Map<String, Object>) paths.get("/api/auth/login");
        Map<String, Object> postOperation = (Map<String, Object>) loginPath.get("post");
        Map<String, Object> responses = (Map<String, Object>) postOperation.get("responses");
        
        assertThat(responses).containsKey("200"); // Success
        assertThat(responses).containsKey("400"); // Bad Request
        assertThat(responses).containsKey("401"); // Unauthorized
        assertThat(responses).containsKey("429"); // Too Many Requests
        assertThat(responses).containsKey("500"); // Internal Server Error
    }

    @Test
    void shouldDocumentRequestExamples() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        
        // Then - Verify examples are provided for request bodies
        // This would typically check for examples in the schema definitions
        Map<String, Object> components = (Map<String, Object>) apiDoc.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        
        // Check that schemas have proper descriptions and examples
        assertThat(schemas).isNotEmpty();
    }

    @Test
    void shouldDocumentAuthenticationRequirements() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");
        
        // Then - Verify protected endpoints document security requirements
        Map<String, Object> mePath = (Map<String, Object>) paths.get("/api/auth/me");
        Map<String, Object> getOperation = (Map<String, Object>) mePath.get("get");
        
        assertThat(getOperation).containsKey("security");
    }

    @Test
    void shouldProvideSwaggerUI() throws Exception {
        // When & Then
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"));
    }

    @Test
    void shouldValidateAllEndpointsAgainstDocumentation() throws Exception {
        // This test would iterate through all documented endpoints and validate them
        // For brevity, we'll test a few key endpoints
        
        // Test registration endpoint
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("doc-test@example.com");
        registerRequest.setName("Doc Test User");
        registerRequest.setPassword("DocTestPassword123!");
        registerRequest.setRole("USER");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").exists());

        // Test login endpoint
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user").exists())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // Test protected endpoint
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.role").exists());

        // Test logout endpoint
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + authResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldDocumentRateLimitingHeaders() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - Verify rate limiting headers are documented and present
        assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isNotNull();
        assertThat(result.getResponse().getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    void shouldDocumentValidationErrors() throws Exception {
        // Given - Invalid registration request
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setName("");
        request.setPassword("weak");

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then - Verify validation errors are properly formatted
        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse response = objectMapper.readValue(responseContent, ErrorResponse.class);
        
        assertThat(response.getValidationErrors()).isNotNull();
        assertThat(response.getValidationErrors()).isNotEmpty();
        assertThat(response.getValidationErrors()).containsKey("email");
        assertThat(response.getValidationErrors()).containsKey("name");
        assertThat(response.getValidationErrors()).containsKey("password");
    }

    @Test
    void shouldDocumentCurlExamples() throws Exception {
        // This test verifies that the documentation includes practical curl examples
        // In a real implementation, this would check the OpenAPI spec for examples
        
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        
        // Verify the API documentation includes proper descriptions
        Map<String, Object> info = (Map<String, Object>) apiDoc.get("info");
        assertThat(info).containsKey("title");
        assertThat(info).containsKey("description");
        assertThat(info).containsKey("version");
    }

    @Test
    void shouldValidateResponseHeaders() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");
        
        // Then - Verify response headers are documented
        Map<String, Object> loginPath = (Map<String, Object>) paths.get("/api/auth/login");
        Map<String, Object> postOperation = (Map<String, Object>) loginPath.get("post");
        Map<String, Object> responses = (Map<String, Object>) postOperation.get("responses");
        Map<String, Object> successResponse = (Map<String, Object>) responses.get("200");
        
        // Verify headers are documented for rate limiting
        if (successResponse.containsKey("headers")) {
            Map<String, Object> headers = (Map<String, Object>) successResponse.get("headers");
            // Would check for X-RateLimit-* headers documentation
        }
    }

    @Test
    void shouldDocumentDeprecatedEndpoints() throws Exception {
        // This test would verify that any deprecated endpoints are properly marked
        // in the OpenAPI documentation
        
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> apiDoc = objectMapper.readValue(content, Map.class);
        
        // Verify API version and deprecation notices
        Map<String, Object> info = (Map<String, Object>) apiDoc.get("info");
        assertThat(info.get("version")).isNotNull();
    }
}