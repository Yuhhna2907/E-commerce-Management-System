package com.codegym.smartphonemanagement.service.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private boolean enabled;
    private boolean locked;
    private LocalDateTime lockoutTime;
    private Integer failedLoginAttempts;
}
