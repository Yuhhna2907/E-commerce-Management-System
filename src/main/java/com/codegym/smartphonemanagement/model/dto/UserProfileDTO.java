package com.codegym.smartphonemanagement.model.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDateTime createdAt;

    // Stats
    private long totalOrders;
    private long totalWishlists;
    private long totalReviews;
    private BigDecimal totalSpent;
}
