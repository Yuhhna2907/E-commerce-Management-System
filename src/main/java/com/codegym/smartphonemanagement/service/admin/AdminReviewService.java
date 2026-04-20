package com.codegym.smartphonemanagement.service.admin;

import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.Review;
import com.codegym.smartphonemanagement.repository.user.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReviewService {

    private final ReviewRepository reviewRepository;

    public Page<Review> searchReviews(String keyword, Integer rating, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        
        return reviewRepository.searchAndFilter(searchKeyword, rating, pageable);
    }

    public boolean toggleReviewStatus(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));
        
        boolean newStatus = !review.getApproved();
        review.setApproved(newStatus);
        reviewRepository.save(review);
        return newStatus;
    }

    public void restoreOrHideReview(Long reviewId, boolean isApproved) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));
        review.setApproved(isApproved);
        reviewRepository.save(review);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", reviewRepository.count());
        stats.put("blockedReviews", reviewRepository.countBlockedReviews());
        stats.put("avgRating", reviewRepository.getOverallAverageRating());
        stats.put("fiveStarReviews", reviewRepository.countFiveStarReviews());
        stats.put("oneStarReviews", reviewRepository.countOneStarReviews());
        return stats;
    }

    public void replyToReview(Long reviewId, String replyMessage) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));
        review.setAdminReply(replyMessage);
        review.setRepliedAt(java.time.LocalDateTime.now());
        reviewRepository.save(review);
    }
}
