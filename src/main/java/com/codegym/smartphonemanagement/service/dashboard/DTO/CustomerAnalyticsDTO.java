package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsDTO {

    // --- KPI HEADER ---
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersThisMonth;
    private Long bannedUsers;

    // --- TAB 1: ACQUISITION & DEMOGRAPHICS ---
    private List<String> growthLabels;
    private List<Long> growthData;

    private Long maleCount;
    private Long femaleCount;
    private Long otherGenderCount;

    private Long genZCount; // < 25
    private Long millennialCount; // 25-40
    private Long genXCount; // > 40

    private List<LocationStat> topLocations;

    // --- TAB 2: RFM & LTV ---
    private BigDecimal averageOrderValue; // AOV
    private BigDecimal averageLifetimeValue; // LTV
    private Long singlePurchaseUsers;
    private Long repeatPurchaseUsers;

    // Bubble chart data for RFM
    // x = frequency (orders), y = monetary (total spent), r = recency (days since last purchase)
    private List<RfmBubble> rfmBubbles;

    // --- TAB 3: LOYALTY ---
    private Long totalPointsLiability;
    private Long pointsEarnedThisMonth;
    private Long pointsRedeemedThisMonth;

    private Long bronzeCount;
    private Long silverCount;
    private Long goldCount;
    private Long diamondCount;

    private Long totalWalletVouchersSaved;

    // --- TAB 4: ENGAGEMENT ---
    private List<UserMetric> topReviewers;
    private List<UserMetric> mostActiveInQA;
    private List<UserMetric> topWishlisters;

    // --- TAB 5: RISKS ---
    private List<UserMetric> highReturnRateUsers; // Top users with cancelled/returned orders
    private List<UserMetric> topSpendersVIPs; // Shown here or in loyalty, with masked info

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationStat {
        private String province;
        private Long userCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfmBubble {
        private String username;
        private Long frequency;
        private BigDecimal monetary;
        private Integer daysSinceLastPurchase; // Recency radius
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetric {
        private Long userId;
        private String username;
        private String maskedEmail;
        private String maskedPhone;
        private String tierOrRole;
        private String metricName;
        private String metricValue;
    }
}
