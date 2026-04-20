package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.Notification;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.NotificationResponseDTO;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<NotificationResponseDTO>> getUnreadNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(user)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/all-json")
    @ResponseBody
    public ResponseEntity<List<NotificationResponseDTO>> getAllNotificationsJson(Authentication authentication) {
        User user = getCurrentUser(authentication);
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
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = getCurrentUser(authentication);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Helper method to get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
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
