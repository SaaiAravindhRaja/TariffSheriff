package com.tariffsheriff.backend.user.repository;

import com.tariffsheriff.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    Optional<User> findByVerificationToken(String verificationToken);
    
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status != 'DELETED'")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
}