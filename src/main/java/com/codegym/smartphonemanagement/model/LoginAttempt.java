package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_attempt_time", columnList = "attemptTime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private LocalDateTime attemptTime;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 45) // Maximum length for IPv6
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @PrePersist
    public void prePersist() {
        if (attemptTime == null) {
            attemptTime = LocalDateTime.now();
        }
    }
}
