package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Compare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Compare entity
 * Manages product comparison data
 */
@Repository
public interface CompareRepository extends JpaRepository<Compare, Long> {
    
    /**
     * Find all compare items for a user
     */
    List<Compare> findByUserIdOrderByAddedAtDesc(Long userId);
    
    /**
     * Find specific compare item by user and product
     */
    Optional<Compare> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Check if product is in user's compare list
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Count compare items for a user
     */
    int countByUserId(Long userId);
    
    /**
     * Delete all compare items for a user
     */
    void deleteByUserId(Long userId);
    
    /**
     * Count compare entries per product for analytics
     * Returns list of [product_id, count] pairs
     */
    @Query("SELECT c.product.id, COUNT(c.id) FROM Compare c GROUP BY c.product.id")
    List<Object[]> countByProduct();

    // Tìm các cặp sản phẩm thường được so sánh cùng nhau
    @Query("SELECT c1.product.name, c2.product.name, COUNT(c1.id) as freq " +
           "FROM Compare c1 JOIN Compare c2 ON c1.user.id = c2.user.id " +
           "WHERE c1.product.id < c2.product.id " +
           "GROUP BY c1.product.id, c2.product.id " +
           "ORDER BY freq DESC")
    List<Object[]> findTopComparePairs(org.springframework.data.domain.Pageable pageable);
}
