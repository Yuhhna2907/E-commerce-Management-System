package com.codegym.smartphonemanagement.service.dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceAnalyticsDTO {

    // --- KPI Header ---
    private Double avgResponseTimeHours;
    private Double slaComplianceRate; // % answers within 24h
    private Double refundCancelRate;
    private Double avgSatisfactionScore; // 1-5 scale based on reviews

    // --- Q&A Analytics ---
    private Long totalQuestions;
    private Long totalAnswers;
    private Long unansweredQuestions;
    
    // Helpfulness distribution (Helpful vs Not Helpful votes)
    private Long totalHelpfulVotes;
    private Long totalNotHelpfulVotes;

    // --- Sentiment Analysis (Ratings 1-5) ---
    private Map<Integer, Long> ratingDistribution;
    private Long totalReviews;

    // --- Lists for UI Grids/Charts ---
    private List<ResponderMetric> topResponders;
    private List<InquiryTopic> hotTopics; // Most asked products
    private List<OrderFailureMetric> recentFailures; // Recent cancelled/refunded orders

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponderMetric {
        private String username;
        private Long answerCount;
        private Double avgResponseTime;
        private Double helpfulnessRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InquiryTopic {
        private Long productId;
        private String productName;
        private Long questionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderFailureMetric {
        private Long orderId;
        private String customerName;
        private String status;
        private String reason;
        private Double amount;
    }
}
