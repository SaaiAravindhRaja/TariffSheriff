package com.tariffsheriff.backend.auth.controller;

import com.tariffsheriff.backend.auth.dto.RegisterRequest;
import com.tariffsheriff.backend.auth.dto.UserResponse;
import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "http://127.0.0.1:8080"})
public class UserController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getAboutMe(),
                    user.getRole(),
                    user.getIsAdmin()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            log.error("Error fetching users: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                    .body("Error: Email is already taken!");
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

            return ResponseEntity.ok(new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getAboutMe(),
                savedUser.getRole(),
                savedUser.getIsAdmin()
            ));

        } catch (Exception e) {
            log.error("User creation error: ", e);
            return ResponseEntity.badRequest()
                .body("Error: User creation failed - " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.badRequest()
                    .body("Error: User not found!");
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok("User deleted successfully");

        } catch (Exception e) {
            log.error("User deletion error: ", e);
            return ResponseEntity.badRequest()
                .body("Error: User deletion failed - " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body("Error: User not found!");
            }

            return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAboutMe(),
                user.getRole(),
                user.getIsAdmin()
            ));

        } catch (Exception e) {
            log.error("Error fetching user: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}