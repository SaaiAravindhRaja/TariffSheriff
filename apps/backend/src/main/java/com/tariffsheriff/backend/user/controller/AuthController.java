package com.tariffsheriff.backend.user.controller;

import com.tariffsheriff.backend.security.JwtTokenProvider;
import com.tariffsheriff.backend.user.dto.AuthResponse;
import com.tariffsheriff.backend.user.dto.LoginRequest;
import com.tariffsheriff.backend.user.dto.RegisterRequest;
import com.tariffsheriff.backend.user.dto.UserDto;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authManager, UserService userService,
                          JwtTokenProvider jwtTokenProvider) {
        this.authManager = authManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())  // pass raw password
                .role(request.getRole() == null ? "USER" : request.getRole())
                .admin(false)
                .build();

        User created = userService.createUser(user); // encoding happens here
        return ResponseEntity.ok(UserDto.fromEntity(created));
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()  // raw password; Spring Security compares with encoded one
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token, authentication.getName(), "USER"));
    }
}
