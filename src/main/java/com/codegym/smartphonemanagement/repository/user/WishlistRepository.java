package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    int countByUserId(Long userId);
    
    /**
     * Find wishlist items with product eagerly loaded to avoid N+1 query
     */
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.addedAt DESC")
    Page<Wishlist> findByUserIdWithProductOrderByAddedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Delete all wishlist items for a user
     */
    void deleteByUserId(Long userId);
    
    /**
     * Count wishlist entries per product for analytics
     * Returns list of [product_id, count] pairs
     */
    @Query("SELECT w.product.id, COUNT(w.id) FROM Wishlist w GROUP BY w.product.id")
    List<Object[]> countByProduct();
}

