package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByUserId(Long userId);

    // --- DASHBOARD METRICS ---
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(l.totalPoints), 0) FROM LoyaltyAccount l")
    long sumExtantPoints();

    // --- BROADCAST FILTERING ---
    @org.springframework.data.jpa.repository.Query("SELECT l.user FROM LoyaltyAccount l WHERE l.lifetimePoints >= :minPoints AND l.lifetimePoints < :maxPoints")
    java.util.List<com.codegym.smartphonemanagement.model.User> findUsersByLifetimePointsBetween(@org.springframework.data.repository.query.Param("minPoints") Integer minPoints, @org.springframework.data.repository.query.Param("maxPoints") Integer maxPoints);

    @org.springframework.data.jpa.repository.Query("SELECT l.user FROM LoyaltyAccount l WHERE l.lifetimePoints >= :minPoints")
    java.util.List<com.codegym.smartphonemanagement.model.User> findUsersByLifetimePointsGreaterThanEqual(@org.springframework.data.repository.query.Param("minPoints") Integer minPoints);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(l) FROM LoyaltyAccount l WHERE l.lifetimePoints >= :min AND l.lifetimePoints < :max")
    long countUsersInPointRange(@org.springframework.data.repository.query.Param("min") Integer min, @org.springframework.data.repository.query.Param("max") Integer max);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(l) FROM LoyaltyAccount l WHERE l.lifetimePoints >= :min")
    long countUsersBeyondPoints(@org.springframework.data.repository.query.Param("min") Integer min);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LoyaltyAccount l ORDER BY l.lifetimePoints DESC")
    java.util.List<LoyaltyAccount> findTopAccounts(org.springframework.data.domain.Pageable pageable);
}
