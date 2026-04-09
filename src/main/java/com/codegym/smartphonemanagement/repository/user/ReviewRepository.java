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
}
