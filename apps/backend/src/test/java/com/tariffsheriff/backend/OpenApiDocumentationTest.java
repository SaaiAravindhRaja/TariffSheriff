package com.tariffsheriff.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class to verify OpenAPI documentation is properly configured
 * Requirement 12.1: OpenAPI/Swagger documentation for authentication endpoints
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class OpenApiDocumentationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    public void testOpenApiDocumentationIsAccessible() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that OpenAPI JSON specification is accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("TariffSheriff Authentication API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    public void testSwaggerUiIsAccessible() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that Swagger UI is accessible
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        
        // Test the actual Swagger UI index page
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"));
    }

    @Test
    public void testAuthenticationEndpointsAreDocumented() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that authentication endpoints are documented in OpenAPI spec
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/refresh']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/verify']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/forgot-password']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/reset-password']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/change-password']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/me']").exists());
    }

    @Test
    public void testUserManagementEndpointsAreDocumented() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that user management endpoints are documented in OpenAPI spec
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{id}/role']").exists())
                .andExpect(jsonPath("$.paths['/api/users/stats']").exists())
                .andExpect(jsonPath("$.paths['/api/users/search']").exists());
    }

    @Test
    public void testSecuritySchemeIsConfigured() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that JWT Bearer security scheme is configured
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    public void testApiTagsAreConfigured() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test that API tags are properly configured
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[?(@.name == 'Authentication')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'User Management')]").exists());
    }
}