package com.codegym.smartphonemanagement.service.admin.dto;

import com.codegym.smartphonemanagement.model.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileDTO {
    // Basic Info
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String address;
    private LocalDateTime createdAt;
    private boolean enabled;
    private boolean locked;
    private LocalDateTime lockoutTime;

    // Loyalty Info
    private int currentPoints;
    private int lifetimePoints;
    
    // Order Summary
    private long totalOrdersCount;
    private BigDecimal totalSpent;
    
    // Recent Orders (Max 5)
    private List<UserRecentOrderDTO> recentOrders;

    @Data
    @Builder
    public static class UserRecentOrderDTO {
        private Long orderId;
        private LocalDateTime orderDate;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private String paymentMethod;
    }
}
