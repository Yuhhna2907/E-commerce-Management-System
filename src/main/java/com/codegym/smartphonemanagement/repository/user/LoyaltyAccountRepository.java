package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByUserId(Long userId);

    // --- DASHBOARD METRICS ---
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(l.totalPoints), 0) FROM LoyaltyAccount l")
    long sumExtantPoints();
}
