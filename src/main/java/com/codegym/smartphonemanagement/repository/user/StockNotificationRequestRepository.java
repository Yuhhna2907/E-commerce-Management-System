package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.entity.StockNotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockNotificationRequestRepository extends JpaRepository<StockNotificationRequest, Long> {
    
    boolean existsByEmailAndProductIdAndVariantId(String email, Long productId, Long variantId);
    
    boolean existsByEmailAndProductIdAndVariantIdIsNull(String email, Long productId);
    
    List<StockNotificationRequest> findByProductIdAndNotifiedFalse(Long productId);
    
    List<StockNotificationRequest> findByProductIdAndVariantIdAndNotifiedFalse(Long productId, Long variantId);
}