package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Find all login attempts for a specific username after a given time
     * @param username the username to search for
     * @param since the time threshold (only attempts after this time are returned)
     * @return list of login attempts
     */
    List<LoginAttempt> findByUsernameAndAttemptTimeAfter(String username, LocalDateTime since);

    /**
     * Find all login attempts from a specific IP address after a given time
     * @param ipAddress the IP address to search for
     * @param since the time threshold (only attempts after this time are returned)
     * @return list of login attempts
     */
    List<LoginAttempt> findByIpAddressAndAttemptTimeAfter(String ipAddress, LocalDateTime since);

    /**
     * Delete all login attempts before a given cutoff date (for cleanup)
     * @param cutoffDate the date before which all attempts should be deleted
     */
    void deleteByAttemptTimeBefore(LocalDateTime cutoffDate);
}
