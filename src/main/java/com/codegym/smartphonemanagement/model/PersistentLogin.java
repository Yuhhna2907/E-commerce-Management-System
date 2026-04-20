package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for storing Remember-Me persistent login tokens.
 * This table is used by Spring Security's JdbcTokenRepositoryImpl.
 */
@Entity
@Table(name = "persistent_logins", indexes = {
    @Index(name = "idx_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersistentLogin {
    
    @Column(nullable = false, length = 64)
    private String username;
    
    @Id
    @Column(length = 64)
    private String series;
    
    @Column(nullable = false, length = 64)
    private String token;
    
    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}
