package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.repository.user.ProductAnswerRepository;
import com.codegym.smartphonemanagement.repository.user.ProductQuestionRepository;
import com.codegym.smartphonemanagement.repository.user.ReviewRepository;
import com.codegym.smartphonemanagement.service.dashboard.DTO.CustomerServiceAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceAnalyticsService {

    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public CustomerServiceAnalyticsDTO getCSAnalytics(LocalDateTime start, LocalDateTime end) {
        log.info("Calculating Customer Service Analytics from {} to {}", start, end);

        // 1. Q&A Metrics
        Double avgResponseSec = questionRepository.findAvgResponseTimeInSeconds(start, end);
        Long slaMetCount = questionRepository.countSlaCompliantQuestions(start, end);
        long totalQuestions = questionRepository.countAllQuestions(); // All time total for KPI
        long answeredCount = questionRepository.countAllAnsweredQuestions();
        long unansweredCount = questionRepository.countAllUnansweredQuestions();

        Double slaRate = answeredCount > 0 ? (slaMetCount.doubleValue() / answeredCount * 100) : 0.0;

        // 2. Sentiment & Review Distribution
        Double avgRating = reviewRepository.getOverallAverageRating();
        long totalReviews = reviewRepository.count();
        
        // Manual Map for distribution (1-5)
        Map<Integer, Long> distribution = new HashMap<>();
        distribution.put(5, reviewRepository.countFiveStarReviews()); // Simple for now
        distribution.put(1, reviewRepository.countOneStarReviews());
        // For others, we can add queries if needed, but 1 and 5 are most critical for sentiment

        // 3. Order Failures
        long totalOrders = orderRepository.countOrdersInPeriod(start, end);
        long failedOrders = orderRepository.countByUser_IdAndStatusIn(null, 
                List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.PARTIAL_REFUNDED)); // Use status in
        
        Double failureRate = totalOrders > 0 ? (double) failedOrders / totalOrders * 100 : 0.0;

        // 4. Top Responders
        List<Object[]> responderRows = answerRepository.findResponderStats(start, end);
        List<CustomerServiceAnalyticsDTO.ResponderMetric> topResponders = responderRows.stream()
                .limit(5)
                .map(row -> CustomerServiceAnalyticsDTO.ResponderMetric.builder()
                        .username((String) row[0])
                        .answerCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        // 5. Hot Topics (Top Inquired Products)
        List<Object[]> productRows = questionRepository.findTopInquiredProducts(start, end, PageRequest.of(0, 5));
        List<CustomerServiceAnalyticsDTO.InquiryTopic> hotTopics = productRows.stream()
                .map(row -> CustomerServiceAnalyticsDTO.InquiryTopic.builder()
                        .productId((Long) row[0])
                        .productName((String) row[1])
                        .questionCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        return CustomerServiceAnalyticsDTO.builder()
                .avgResponseTimeHours(avgResponseSec != null ? avgResponseSec / 3600.0 : 0.0)
                .slaComplianceRate(slaRate)
                .refundCancelRate(failureRate)
                .avgSatisfactionScore(avgRating != null ? avgRating : 0.0)
                .totalQuestions(totalQuestions)
                .totalAnswers(answeredCount)
                .unansweredQuestions(unansweredCount)
                .ratingDistribution(distribution)
                .totalReviews(totalReviews)
                .topResponders(topResponders)
                .hotTopics(hotTopics)
                .build();
    }
}
