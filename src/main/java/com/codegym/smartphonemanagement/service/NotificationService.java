package com.codegym.smartphonemanagement.service;

import com.codegym.smartphonemanagement.model.Notification;
import com.codegym.smartphonemanagement.model.NotificationType;
import com.codegym.smartphonemanagement.model.User;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(User user, String message, NotificationType type, String relatedUrl);
    List<Notification> getUnreadNotifications(User user);
    List<Notification> getAllNotifications(User user);
    void markAsRead(Long notificationId);
    void markAllAsRead(User user);
}
