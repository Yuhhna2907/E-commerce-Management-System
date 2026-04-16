package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.entity.SavedForLater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedForLaterRepository extends JpaRepository<SavedForLater, Long> {
    
    /**
     * Lấy tất cả items đã lưu của user
     */
    List<SavedForLater> findByUserIdOrderBySavedAtDesc(Long userId);
    
    /**
     * Đếm số lượng items đã lưu của user
     */
    long countByUserId(Long userId);
    
    /**
     * Kiểm tra xem product đã được lưu chưa
     */
    boolean existsByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);
    
    /**
     * Tìm saved item theo userId, productId, variantId
     */
    Optional<SavedForLater> findByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);
    
    /**
     * Xóa tất cả saved items của user
     */
    void deleteByUserId(Long userId);
}
