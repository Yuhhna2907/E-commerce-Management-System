package com.codegym.smartphonemanagement.model.dto;

import com.codegym.smartphonemanagement.model.AdminNotificationType;
import com.codegym.smartphonemanagement.model.NotificationPriority;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminNotificationResponseDTO {
    private Long id;
    private String title;
    private String message;
    private AdminNotificationType type;
    private NotificationPriority priority;
    private String targetUrl;

    @JsonProperty("isRead")
    private boolean isRead;

    private String timeAgo;
}
