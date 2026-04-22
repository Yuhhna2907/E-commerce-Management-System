package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndApprovedTrue(Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("""
           SELECT AVG(r.rating)
           FROM Review r
           WHERE r.product.id = :productId
           """)
    Double getAverageRating(Long productId);

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Performance optimization: Count reviews by user without loading all reviews
    long countByUserId(Long userId);

    @Query("SELECT r FROM Review r WHERE " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:keyword IS NULL OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.user.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Review> searchAndFilter(
            @org.springframework.data.repository.query.Param("keyword") String keyword, 
            @org.springframework.data.repository.query.Param("rating") Integer rating, 
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.approved = false")
    long countBlockedReviews();

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getOverallAverageRating();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating = 5")
    long countFiveStarReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating = 1")
    long countOneStarReviews();

    // --- ANALYTICS ---

    @Query("SELECT r.product.id, AVG(r.rating), COUNT(r.id) " +
           "FROM Review r GROUP BY r.product.id")
    List<Object[]> countRatingsByProduct();

    @Query("SELECT r.product.id, COUNT(r.id) " +
           "FROM Review r WHERE r.rating <= 2 " +
           "GROUP BY r.product.id")
    List<Object[]> findProductsWithNegativeReviews();
}
