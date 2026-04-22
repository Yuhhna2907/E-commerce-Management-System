package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // --- DASHBOARD METRICS ---
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_USER'")
    long countStandardUsers();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_USER'")
    java.util.List<User> findStandardUsers();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false OR u.failedLoginAttempts > 0")
    long countBannedOrRiskyUsers();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countNewUsersSince(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);

    @org.springframework.data.jpa.repository.Query("SELECT u.gender, COUNT(u) FROM User u GROUP BY u.gender")
    java.util.List<Object[]> countByGender();

    @org.springframework.data.jpa.repository.Query("SELECT DATE(u.createdAt), COUNT(u) FROM User u " +
            "WHERE u.createdAt >= :startDate AND u.createdAt <= :endDate " +
            "GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt) ASC")
    java.util.List<Object[]> getUserRegistrationTrend(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate, 
                                                      @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    @org.springframework.data.jpa.repository.Query("SELECT u.dateOfBirth FROM User u WHERE u.dateOfBirth IS NOT NULL")
    java.util.List<java.time.LocalDate> getAllUserBirthDates();
}
