package com.tariffsheriff.backend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void healthCheck_isPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUI_isPermitted() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void optionsRequest_isPermitted() throws Exception {
        // FIX: Add "Origin" and "Access-Control-Request-Method" headers
        // to simulate a real CORS preflight request.
        mockMvc.perform(options("/api/any-endpoint")
                        .header("Origin", "http://localhost:3000") 
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk()); // Now it should return 200 OK
    }

    @Test
    void apiAuthEndpoint_isPermitted() throws Exception {
        // Assumes /api/auth/login exists. A 404 NOT 401 proves it's permitted.
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isNotFound()); // or isOk() if it's a real endpoint
    }

    @Test
    void securedApiEndpoint_isUnauthorized_withoutToken() throws Exception {
        mockMvc.perform(get("/api/some-secured-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securedApiEndpoint_isOk_withValidToken() throws Exception {
        // We send a request with a valid (mocked) JWT.
        // It passes security (not 401) and will likely 404, which is a success.
        mockMvc.perform(get("/api/some-secured-endpoint")
                        .with(jwt()))
                .andExpect(status().isNotFound()); // Proves it was not 401 Unauthorized
    }

    @Test
    void unknownRootEndpoint_isDenied() throws Exception {
        // FIX: The correct status for an *unauthenticated* request
        // to a protected endpoint is 401 (Unauthorized).
        mockMvc.perform(get("/some-other-random-path"))
                .andExpect(status().isUnauthorized()); // Changed from isForbidden()
    }
}