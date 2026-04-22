package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.dto.RestockDemandDTO;
import com.codegym.smartphonemanagement.model.entity.StockNotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockNotificationRequestRepository extends JpaRepository<StockNotificationRequest, Long> {
    
    boolean existsByEmailAndProductIdAndVariantId(String email, Long productId, Long variantId);
    
    boolean existsByEmailAndProductIdAndVariantIdIsNull(String email, Long productId);
    
    List<StockNotificationRequest> findByProductIdAndNotifiedFalse(Long productId);
    
    List<StockNotificationRequest> findByProductIdAndVariantIdAndNotifiedFalse(Long productId, Long variantId);
    
    /**
     * Get top N products by notification count (for out-of-stock products only)
     * Used for Restock Intelligence Widget on dashboard
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.RestockDemandDTO(
            p.id, p.name, p.imageUrl, c.name, 'Hết hàng',
            CAST(COUNT(sn.id) AS int), p.updatedAt
        )
        FROM StockNotificationRequest sn
        JOIN Product p ON sn.productId = p.id
        LEFT JOIN p.category c
        WHERE p.stock = 0 AND p.active = true
        GROUP BY p.id, p.name, p.imageUrl, c.name, p.updatedAt
        ORDER BY COUNT(sn.id) DESC
    """)
    List<RestockDemandDTO> findTop5ByNotificationCount(Pageable pageable);
    
    /**
     * Get paginated restock demand data with filtering
     * Used for Restock Demand detail page
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.RestockDemandDTO(
            p.id, p.name, p.imageUrl, c.name, 'Hết hàng',
            CAST(COUNT(sn.id) AS int), p.updatedAt
        )
        FROM StockNotificationRequest sn
        JOIN Product p ON sn.productId = p.id
        LEFT JOIN p.category c
        WHERE p.stock = 0 AND p.active = true
        AND (:categoryId IS NULL OR c.id = :categoryId)
        GROUP BY p.id, p.name, p.imageUrl, c.name, p.updatedAt
    """)
    Page<RestockDemandDTO> findRestockDemandData(
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );
    
    /**
     * Get all restock demand data for export (no pagination)
     */
    @Query("""
        SELECT new com.codegym.smartphonemanagement.model.dto.RestockDemandDTO(
            p.id, p.name, p.imageUrl, c.name, 'Hết hàng',
            CAST(COUNT(sn.id) AS int), p.updatedAt
        )
        FROM StockNotificationRequest sn
        JOIN Product p ON sn.productId = p.id
        LEFT JOIN p.category c
        WHERE p.stock = 0 AND p.active = true
        AND (:categoryId IS NULL OR c.id = :categoryId)
        GROUP BY p.id, p.name, p.imageUrl, c.name, p.updatedAt
        ORDER BY COUNT(sn.id) DESC
    """)
    List<RestockDemandDTO> findAllRestockDemandData(@Param("categoryId") Long categoryId);
}