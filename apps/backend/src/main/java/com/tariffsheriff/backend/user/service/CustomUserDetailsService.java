package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details from the database and creates Spring Security UserDetails objects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email address for Spring Security authentication.
     *
     * @param email User's email address
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        log.debug("Found user: {} with role: {} and status: {}", 
                user.getEmail(), user.getRole(), user.getStatus());

        return createUserDetails(user);
    }

    /**
     * Create Spring Security UserDetails from our User entity.
     *
     * @param user User entity
     * @return UserDetails for Spring Security
     */
    private UserDetails createUserDetails(User user) {
        // Create authorities based on user role
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.isAccountLocked())
                .credentialsExpired(false)
                .disabled(!isUserEnabled(user))
                .build();
    }

    /**
     * Determine if user account is enabled based on status and verification.
     *
     * @param user User entity
     * @return true if user is enabled, false otherwise
     */
    private boolean isUserEnabled(User user) {
        // User is enabled if:
        // 1. Status is ACTIVE
        // 2. Email is verified (or verification is not required for existing users)
        // 3. Account is not locked
        return user.getStatus() == UserStatus.ACTIVE && 
               user.isEmailVerified() && 
               !user.isAccountLocked();
    }
}
