package com.tariffsheriff.backend.auth.controller;

import com.tariffsheriff.backend.auth.dto.RegisterRequest;
import com.tariffsheriff.backend.auth.dto.UserResponse;
import com.tariffsheriff.backend.auth.entity.User;
import com.tariffsheriff.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserController controller;

    @Test
    void getAllUsers_returnsMappedResponses() {
        User u = new User();
        u.setId(5L);
        u.setName("Test");
        u.setEmail("t@example.com");
        u.setAboutMe("x");
        u.setRole("USER");
        u.setIsAdmin(false);

        when(userRepository.findAll()).thenReturn(List.of(u));

        ResponseEntity<List<UserResponse>> resp = controller.getAllUsers();
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(5L, resp.getBody().get(0).getId());
    }

    @Test
    void createUser_returnsBadRequest_whenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("a@b.com");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        ResponseEntity<?> resp = controller.createUser(req);
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().toString().contains("Email is already taken"));
    }

    @Test
    void createUser_savesAndReturnsUserResponse() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@e.com");
        req.setName("New");
        req.setPassword("pw");

        when(userRepository.existsByEmail("new@e.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        User saved = new User();
        saved.setId(11L);
        saved.setName("New");
        saved.setEmail("new@e.com");
        saved.setIsAdmin(false);

        when(userRepository.save(any())).thenReturn(saved);

        ResponseEntity<?> resp = controller.createUser(req);
        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody() instanceof UserResponse);
        UserResponse ur = (UserResponse) resp.getBody();
        assertEquals(11L, ur.getId());
    }

    @Test
    void deleteUser_returnsBadRequest_whenNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);
        ResponseEntity<?> resp = controller.deleteUser(99L);
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().toString().contains("User not found"));
    }

    @Test
    void getUserById_returnsUserResponse_whenFound() {
        User u = new User();
        u.setId(7L);
        u.setName("U");
        u.setEmail("u@e");
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        ResponseEntity<?> resp = controller.getUserById(7L);
        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody() instanceof UserResponse);
        UserResponse ur = (UserResponse) resp.getBody();
        assertEquals(7L, ur.getId());
    }
}
