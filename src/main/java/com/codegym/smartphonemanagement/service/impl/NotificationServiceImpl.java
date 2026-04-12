package com.codegym.smartphonemanagement.service.impl;

import com.codegym.smartphonemanagement.model.Notification;
import com.codegym.smartphonemanagement.model.NotificationType;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.NotificationRepository;
import com.codegym.smartphonemanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification sendNotification(User user, String message, NotificationType type, String relatedUrl) {
        // Save to DB
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(type)
                .relatedUrl(relatedUrl)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        // Send via WebSocket (STOMP)
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", savedNotification.getId());
        payload.put("message", savedNotification.getMessage());
        payload.put("type", savedNotification.getType().toString());
        payload.put("relatedUrl", savedNotification.getRelatedUrl());
        payload.put("createdAt", savedNotification.getCreatedAt());

        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
        payload.put("unreadCount", unreadCount);

        messagingTemplate.convertAndSend(
                "/topic/user/" + user.getUsername() + "/notifications",
                payload
        );

        return savedNotification;
    }

    @Override
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Override
    public List<Notification> getAllNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadList = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        unreadList.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unreadList);
    }
}
