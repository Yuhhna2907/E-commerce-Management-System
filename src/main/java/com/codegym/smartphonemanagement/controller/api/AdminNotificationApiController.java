package com.codegym.smartphonemanagement.controller.api;

import com.codegym.smartphonemanagement.model.AdminNotification;
import com.codegym.smartphonemanagement.model.dto.AdminNotificationResponseDTO;
import com.codegym.smartphonemanagement.service.notification.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
public class AdminNotificationApiController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        List<AdminNotification> notifications = adminNotificationService.getLatestNotifications();
        long unreadCount = adminNotificationService.getUnreadCount();

        List<AdminNotificationResponseDTO> dtos = notifications.stream()
                .map(n -> AdminNotificationResponseDTO.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .type(n.getType())
                        .priority(n.getPriority())
                        .targetUrl(n.getTargetUrl())
                        .isRead(n.isRead())
                        .timeAgo(getTimeAgo(n.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", dtos);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        adminNotificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        adminNotificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "Vừa xong";
        if (seconds < 3600) return (seconds / 60) + " phút trước";
        if (seconds < 86400) return (seconds / 3600) + " giờ trước";
        return (seconds / 86400) + " ngày trước";
    }
}
