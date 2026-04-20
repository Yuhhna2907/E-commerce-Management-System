package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // --- DASHBOARD METRICS ---
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_USER'")
    long countStandardUsers();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_USER'")
    java.util.List<User> findStandardUsers();
}