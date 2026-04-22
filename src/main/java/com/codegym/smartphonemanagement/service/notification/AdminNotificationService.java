package com.codegym.smartphonemanagement.service.notification;

import com.codegym.smartphonemanagement.model.AdminNotification;
import com.codegym.smartphonemanagement.model.AdminNotificationType;
import com.codegym.smartphonemanagement.model.NotificationPriority;
import com.codegym.smartphonemanagement.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    public void notify(String title, String message, AdminNotificationType type, NotificationPriority priority, String targetUrl) {
        log.info("Sending admin notification: {} - {}", title, type);
        AdminNotification notification = AdminNotification.builder()
                .title(title)
                .message(message)
                .type(type)
                .priority(priority)
                .targetUrl(targetUrl)
                .build();
        adminNotificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<AdminNotification> getLatestNotifications() {
        return adminNotificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AdminNotification> getAllNotifications() {
        return adminNotificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return adminNotificationRepository.countByIsReadFalse();
    }

    public void markAsRead(Long id) {
        adminNotificationRepository.markAsRead(id);
    }

    public void markAllAsRead() {
        adminNotificationRepository.markAllAsRead();
    }
}
