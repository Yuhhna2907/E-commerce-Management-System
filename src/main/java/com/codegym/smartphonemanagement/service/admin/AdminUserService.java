package com.codegym.smartphonemanagement.service.admin;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.Role;
import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import com.codegym.smartphonemanagement.model.PointTransaction;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.repository.user.LoyaltyAccountRepository;
import com.codegym.smartphonemanagement.repository.user.PointTransactionRepository;
import com.codegym.smartphonemanagement.service.admin.dto.AdminUserDTO;
import com.codegym.smartphonemanagement.service.admin.dto.UserProfileDTO;
import com.codegym.smartphonemanagement.service.admin.dto.UserStatsDTO;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public List<AdminUserDTO> getAllStandardUsers() {
        // Chỉ lấy user thường, ngoại trừ ROLE_ADMIN
        return userRepository.findAll().stream()
                .filter(u -> hasRoleUser(u) && !hasRoleAdmin(u))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserStatsDTO getUserStats() {
        List<User> all = userRepository.findAll().stream()
                .filter(u -> hasRoleUser(u) && !hasRoleAdmin(u))
                .toList();

        long total = all.size();
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newThisMonth = all.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        long banned = all.stream()
                .filter(u -> !u.isEnabled() || u.isAccountLocked())
                .count();

        return UserStatsDTO.builder()
                .totalUsers(total)
                .newUsersThisMonth(newThisMonth)
                .bannedUsers(banned)
                .build();
    }

    @Transactional
    public boolean toggleUserLock(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User ID: " + id));

        if (hasRoleAdmin(user)) {
            throw new IllegalStateException("Hành động bị cấm! Bạn không thể khóa một Administrator.");
        }

        boolean isCurrentlyLocked = !user.isEnabled() || user.isAccountLocked();
        
        if (isCurrentlyLocked) {
            // UNBAN
            user.setEnabled(true);
            user.setLockoutTime(null);
            user.setFailedLoginAttempts(0);
            log.info("UNBANNED user id: {}", id);
        } else {
            // BAN
            user.setEnabled(false);
            // Có thể gài lockoutTime xa ở tương lai
            user.setLockoutTime(LocalDateTime.now().plusYears(100));
            log.info("BANNED user id: {}", id);
        }

        userRepository.save(user);
        return !isCurrentlyLocked; // Trả về trạng thái mới: true = bị khóa
    }

    @Transactional
    public void adjustLoyaltyPoints(Long userId, int points, String reason) {
        if (points == 0) return;
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
        LoyaltyAccount loyaltyAcc = loyaltyAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Loyalty account not found"));

        int newBalance = loyaltyAcc.getTotalPoints() + points;
        if (newBalance < 0) {
            throw new IllegalArgumentException("Không thể trừ quá điểm hiện có. Số dư tối đa có thể trừ: " + loyaltyAcc.getTotalPoints());
        }

        loyaltyAcc.setTotalPoints(newBalance);
        if (points > 0) {
            loyaltyAcc.setLifetimePoints(loyaltyAcc.getLifetimePoints() + points);
        }
        
        loyaltyAccountRepository.save(loyaltyAcc);

        // Lưu PointTransaction
        PointTransaction pt = PointTransaction.builder()
                .user(user)
                .points(points)
                .type(com.codegym.smartphonemanagement.model.PointTransactionType.ADMIN_ADJUST)
                .description("[ADMIN MANUAL] " + reason)
                .build();
        pointTransactionRepository.save(pt);
        
        log.info("Admin adjusted {} points for user {}, reason: {}", points, userId, reason);
    }

    public UserProfileDTO getUserProfileDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        LoyaltyAccount loyaltyAcc = loyaltyAccountRepository.findByUserId(userId).orElse(null);
        int currentPoints = loyaltyAcc != null ? loyaltyAcc.getTotalPoints() : 0;
        int lifetimePoints = loyaltyAcc != null ? loyaltyAcc.getLifetimePoints() : 0;

        List<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        
        long totalOrdersCount = orders.size();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus().name()) || "PARTIAL_REFUNDED".equals(o.getStatus().name()))
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UserProfileDTO.UserRecentOrderDTO> recentOrders = orders.stream()
                .limit(5)
                .map(o -> UserProfileDTO.UserRecentOrderDTO.builder()
                        .orderId(o.getId())
                        .orderDate(o.getCreatedAt())
                        .status(o.getStatus())
                        .totalPrice(o.getTotalPrice())
                        .paymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null)
                        .build())
                .collect(Collectors.toList());

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .locked(user.isAccountLocked())
                .lockoutTime(user.getLockoutTime())
                .currentPoints(currentPoints)
                .lifetimePoints(lifetimePoints)
                .totalOrdersCount(totalOrdersCount)
                .totalSpent(totalSpent)
                .recentOrders(recentOrders)
                .build();
    }

    private boolean hasRoleUser(User u) {
        for (Role r : u.getRoles()) {
            if ("ROLE_USER".equals(r.getName())) return true;
        }
        return false;
    }

    private boolean hasRoleAdmin(User u) {
        for (Role r : u.getRoles()) {
            if ("ROLE_ADMIN".equals(r.getName())) return true;
        }
        return false;
    }

    private AdminUserDTO toDTO(User user) {
        return AdminUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .locked(user.isAccountLocked())
                .lockoutTime(user.getLockoutTime())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .build();
    }
}
