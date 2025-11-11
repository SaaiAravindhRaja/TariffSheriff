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
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void createUser_setsRoleAndAdminCorrectly() {
        // This test verifies the non-default paths for Role and Admin
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@e.com");
        req.setName("Admin User");
        req.setPassword("pw");
        req.setRole("ADMIN"); // Explicitly set role
        req.setIsAdmin(true); // Explicitly set admin

        when(userRepository.existsByEmail("admin@e.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC_ADMIN");

        // Use ArgumentCaptor to capture the user object sent to save()
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // We need to mock the save to return the captured object
        when(userRepository.save(userCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        controller.createUser(req);

        // Verify the captured user has the correct, non-default values
        User capturedUser = userCaptor.getValue();
        assertEquals("ADMIN", capturedUser.getRole());
        assertTrue(capturedUser.getIsAdmin());
        assertEquals("ENC_ADMIN", capturedUser.getPassword());
    }

    @Test
    void createUser_defaultsRoleAndAdminWhenNullOrBlank() {
        // This test verifies the default paths for Role and Admin
        RegisterRequest req = new RegisterRequest();
        req.setEmail("default@e.com");
        req.setName("Default User");
        req.setPassword("pw");
        req.setRole("   "); // Set a blank role to test the isBlank() check
        req.setIsAdmin(null); // Set null to test the null check

        when(userRepository.existsByEmail("default@e.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC_DEFAULT");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        controller.createUser(req);

        // Verify the captured user has the correct default values
        User capturedUser = userCaptor.getValue();
        assertEquals("USER", capturedUser.getRole()); // Should default to USER
        assertFalse(capturedUser.getIsAdmin()); // Should default to false
    }

    @Test
    void getUserById_returnsBadRequest_whenNotFound() {
        // This was a missing test for the getUserById endpoint
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        ResponseEntity<?> resp = controller.getUserById(99L);
        
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().toString().contains("User not found!"));
    }

    // --- Tests for the catch (Exception e) blocks ---

    @Test
    void getAllUsers_handlesException() {
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB Connection Failed"));
        
        ResponseEntity<List<UserResponse>> resp = controller.getAllUsers();
        
        assertEquals(400, resp.getStatusCodeValue());
        assertNull(resp.getBody()); // .build() returns a null body
    }

    @Test
    void createUser_handlesException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("fail@e.com");
        req.setPassword("pw");

        when(userRepository.existsByEmail("fail@e.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Save Failed"));

        ResponseEntity<?> resp = controller.createUser(req);
        
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().toString().contains("Error: User creation failed - Save Failed"));
    }

    @Test
    void deleteUser_handlesException() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("Delete Failed")).when(userRepository).deleteById(1L);

        ResponseEntity<?> resp = controller.deleteUser(1L);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().toString().contains("Error: User deletion failed - Delete Failed"));
    }

    @Test
    void getUserById_handlesException() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("DB Connection Failed"));

        ResponseEntity<?> resp = controller.getUserById(1L);

        assertEquals(400, resp.getStatusCodeValue());
        assertNull(resp.getBody());
    }
}
