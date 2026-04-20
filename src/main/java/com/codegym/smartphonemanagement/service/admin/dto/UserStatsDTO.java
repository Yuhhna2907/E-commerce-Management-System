package com.codegym.smartphonemanagement.service.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsDTO {
    private long totalUsers;
    private long newUsersThisMonth;
    private long bannedUsers;
}
