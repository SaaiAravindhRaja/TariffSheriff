package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JWT Authentication Filter for request interception and token validation.
 * This filter extracts JWT tokens from Authorization headers, validates them,
 * and populates the security context with authenticated user information.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT token from request
            String token = extractTokenFromRequest(request);
            
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateToken(token, request);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication in security context: {}", e.getMessage());
            // Clear security context on error
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     *
     * @param request HTTP request
     * @return JWT token string or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("Extracted JWT token from Authorization header for request: {}", request.getRequestURI());
            return token;
        }
        
        return null;
    }

    /**
     * Authenticate the JWT token and set security context.
     *
     * @param token JWT token
     * @param request HTTP request for additional details
     */
    private void authenticateToken(String token, HttpServletRequest request) {
        try {
            // Extract token information
            TokenInfo tokenInfo = tokenService.extractTokenInfo(token);
            
            // Check if token is expired
            if (tokenInfo.isExpired()) {
                log.debug("JWT token is expired for user: {}", tokenInfo.getUsername());
                return;
            }

            // Check if token is an access token
            if (!JwtUtil.ACCESS_TOKEN_TYPE.equals(tokenInfo.getTokenType())) {
                log.debug("Token is not an access token: {}", tokenInfo.getTokenType());
                return;
            }

            // Check if token is blacklisted
            if (tokenService.isTokenBlacklisted(token)) {
                log.debug("JWT token is blacklisted for user: {}", tokenInfo.getUsername());
                return;
            }

            // Load user from database
            Optional<User> userOptional = userRepository.findByEmail(tokenInfo.getUsername());
            if (userOptional.isEmpty()) {
                log.debug("User not found for token: {}", tokenInfo.getUsername());
                return;
            }

            User user = userOptional.get();

            // Validate token against user
            if (!tokenService.validateAccessToken(token, user)) {
                log.debug("Token validation failed for user: {}", user.getEmail());
                return;
            }

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = createAuthenticationToken(user, token);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Successfully authenticated user: {} for request: {}", 
                    user.getEmail(), request.getRequestURI());

        } catch (JwtException e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error during token authentication: {}", e.getMessage());
        }
    }

    /**
     * Create Spring Security authentication token from user and JWT.
     *
     * @param user Authenticated user
     * @param jwtToken JWT token
     * @return UsernamePasswordAuthenticationToken
     */
    private UsernamePasswordAuthenticationToken createAuthenticationToken(User user, String jwtToken) {
        // Create authorities based on user role
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Create authentication token with user details
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(), // principal (username)
                null, // credentials (we don't store password in context)
                authorities // authorities/roles
        );
    }

    /**
     * Determine if this filter should be applied to the request.
     * Skip authentication for public endpoints.
     *
     * @param request HTTP request
     * @return true if filter should be skipped, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip authentication for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/actuator/health") ||
               path.equals("/error");
    }
}