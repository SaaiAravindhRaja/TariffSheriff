package com.tariffsheriff.backend.auth.controller;

import com.tariffsheriff.backend.auth.dto.AuthResponse;
import com.tariffsheriff.backend.auth.dto.LoginRequest;
import com.tariffsheriff.backend.auth.dto.RegisterRequest;
import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import com.tariffsheriff.backend.auth.service.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "http://127.0.0.1:8080"})
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is already taken"));
            }

            // Create new user
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setAboutMe(request.getAboutMe());
            user.setRole(request.getRole());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setIsAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false);

            User savedUser = userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(savedUser);

            return ResponseEntity.ok(new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getIsAdmin()
            ));

        } catch (Exception e) {
            log.error("Registration error: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for email: {}", request.getEmail());
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            log.info("Authentication successful for: {}", request.getEmail());
            User user = (User) authentication.getPrincipal();
            
            // Generate JWT token
            log.info("Generating JWT token for user: {}", user.getEmail());
            String token = jwtTokenProvider.generateToken(user);
            log.info("JWT token generated successfully");

            return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getIsAdmin()
            ));

        } catch (Exception e) {
            log.error("Login error for {}: ", request.getEmail(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid credentials"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtTokenProvider.extractUsername(token);
                
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && jwtTokenProvider.isTokenValid(token, user)) {
                    return ResponseEntity.ok(new AuthResponse(
                        token,
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getIsAdmin()
                    ));
                }
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
        } catch (Exception e) {
            log.error("Token validation error: ", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Token validation failed"));
        }
    }
}