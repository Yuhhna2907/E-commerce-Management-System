package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Notification;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.NotificationResponseDTO;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.NotificationService;
import com.codegym.smartphonemanagement.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/notifications")
@RequiredArgsConstructor
public class lUserNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/unread")
    @ResponseBody
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications() {
        User user = SecurityUtil.getCurrentUser(userRepository);
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(user)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/all-json")
    @ResponseBody
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotificationsJson() {
        User user = SecurityUtil.getCurrentUser(userRepository);
        List<NotificationResponseDTO> notifications = notificationService.getAllNotifications(user)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Void> markAllAsRead() {
        User user = SecurityUtil.getCurrentUser(userRepository);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    private NotificationResponseDTO mapToDTO(Notification notif) {
        return NotificationResponseDTO.builder()
                .id(notif.getId())
                .message(notif.getMessage())
                .type(notif.getType())
                .isRead(notif.isRead())
                .relatedUrl(notif.getRelatedUrl())
                .createdAt(notif.getCreatedAt())
                .build();
    }
}
