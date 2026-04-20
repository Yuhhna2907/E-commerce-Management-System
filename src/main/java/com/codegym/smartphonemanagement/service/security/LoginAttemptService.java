package com.codegym.smartphonemanagement.service.security;

import com.codegym.smartphonemanagement.model.LoginAttempt;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for tracking login attempts and managing account lockout logic
 */
public interface LoginAttemptService {

    /**
     * Record a login attempt (successful or failed)
     * @param username the username attempting to log in
     * @param success whether the login was successful
     * @param ipAddress the IP address of the client
     * @param userAgent the user agent string from the client
     */
    void recordLoginAttempt(String username, boolean success, String ipAddress, String userAgent);

    /**
     * Check if an account is currently locked
     * @param username the username to check
     * @return true if the account is locked, false otherwise
     */
    boolean isAccountLocked(String username);

    /**
     * Reset the failed login attempts counter for a user
     * @param username the username to reset
     */
    void resetFailedAttempts(String username);

    /**
     * Lock an account for a specified duration
     * @param username the username to lock
     */
    void lockAccount(String username);

    /**
     * Unlock an account
     * @param username the username to unlock
     */
    void unlockAccount(String username);

    /**
     * Get failed login attempts for a user since a given time
     * @param username the username to query
     * @param since the time threshold
     * @return list of failed login attempts
     */
    List<LoginAttempt> getFailedAttemptsForUser(String username, LocalDateTime since);

    /**
     * Get all login attempts from a specific IP address since a given time
     * @param ipAddress the IP address to query
     * @param since the time threshold
     * @return list of login attempts
     */
    List<LoginAttempt> getAttemptsFromIp(String ipAddress, LocalDateTime since);

    /**
     * Clean up old login attempts (older than 90 days)
     * This method should be called periodically to prevent database bloat
     */
    void cleanupOldAttempts();
}
