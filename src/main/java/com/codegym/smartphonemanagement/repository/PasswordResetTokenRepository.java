package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for password reset token operations
 * Requirements: 3.8, 4.2, 5.8, 5.12
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * Find a password reset token by its token string
     * @param token the token string to search for
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Delete expired and used tokens for cleanup
     * Removes tokens that have expired or been used
     * @param now the current timestamp for comparison
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.usedAt IS NOT NULL")
    void deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);
    
    /**
     * Count recent password reset requests by email for rate limiting
     * @param email the user's email address
     * @param since the timestamp to count from (e.g., 1 hour ago)
     * @return count of recent token requests
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.email = :email AND t.createdAt > :since")
    int countRecentTokensByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
}
