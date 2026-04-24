package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.PinnedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PinnedProduct entity
 * Manages manually pinned products for homepage display
 */
@Repository
public interface PinnedProductRepository extends JpaRepository<PinnedProduct, Long> {
    
    /**
     * Find all active pinned products ordered by display order
     * Used for homepage display
     */
    @Query("SELECT pp FROM PinnedProduct pp JOIN FETCH pp.product WHERE pp.active = true ORDER BY pp.displayOrder ASC")
    List<PinnedProduct> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Count active pinned products
     * Used to enforce maximum limit (10 products)
     */
    int countByActiveTrue();
    
    /**
     * Check if a product is already pinned and active
     */
    boolean existsByProductIdAndActiveTrue(Long productId);
}
