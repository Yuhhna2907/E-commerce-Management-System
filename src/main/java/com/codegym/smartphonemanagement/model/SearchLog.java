package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking user search behavior
 * Used for search trend analytics and conversion tracking
 */
@Entity
@Table(name = "search_log", indexes = {
    @Index(name = "idx_search_keyword", columnList = "search_keyword"),
    @Index(name = "idx_search_timestamp", columnList = "search_timestamp"),
    @Index(name = "idx_session_id", columnList = "session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // nullable for anonymous users
    
    @Column(name = "search_keyword", nullable = false, length = 255)
    private String searchKeyword; // stored in lowercase
    
    @Column(name = "search_timestamp", nullable = false)
    private LocalDateTime searchTimestamp;
    
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    @Column(name = "converted", nullable = false)
    private Boolean converted = false;
    
    @Column(name = "conversion_timestamp")
    private LocalDateTime conversionTimestamp;
    
    @PrePersist
    protected void onCreate() {
        if (searchTimestamp == null) {
            searchTimestamp = LocalDateTime.now();
        }
        if (converted == null) {
            converted = false;
        }
    }
}
