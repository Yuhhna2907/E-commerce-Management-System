package com.codegym.smartphonemanagement.model.dto;

import com.codegym.smartphonemanagement.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {
    private Long id;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private String relatedUrl;
    private LocalDateTime createdAt;
}
