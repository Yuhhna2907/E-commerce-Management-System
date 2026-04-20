package com.codegym.smartphonemanagement.service.notification;

import com.codegym.smartphonemanagement.model.dto.SaveForLaterResponse;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductVariant;
import com.codegym.smartphonemanagement.model.entity.StockNotificationRequest;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.ProductVariantRepository;
import com.codegym.smartphonemanagement.repository.user.StockNotificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockNotificationService {
    
    private final StockNotificationRequestRepository stockNotificationRequestRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    
    @Transactional
    public SaveForLaterResponse registerNotification(Long productId, Long variantId, String email) {
        try {
            // Validate product exists
            Product product = productRepository.findById(productId)
                    .orElse(null);
            
            if (product == null) {
                return SaveForLaterResponse.builder()
                        .success(false)
                        .message("Sản phẩm không tồn tại")
                        .build();
            }
            
            // Validate variant if provided
            if (variantId != null) {
                ProductVariant variant = productVariantRepository.findById(variantId)
                        .orElse(null);
                
                if (variant == null || !variant.getProduct().getId().equals(productId)) {
                    return SaveForLaterResponse.builder()
                            .success(false)
                            .message("Phiên bản sản phẩm không tồn tại")
                            .build();
                }
                
                // Check if product is actually out of stock
                if (variant.getStockQuantity() != null && variant.getStockQuantity() > 0) {
                    return SaveForLaterResponse.builder()
                            .success(false)
                            .message("Sản phẩm hiện đang có sẵn")
                            .build();
                }
            }
            
            // Check for duplicate registration
            boolean exists;
            if (variantId != null) {
                exists = stockNotificationRequestRepository.existsByEmailAndProductIdAndVariantId(email, productId, variantId);
            } else {
                exists = stockNotificationRequestRepository.existsByEmailAndProductIdAndVariantIdIsNull(email, productId);
            }
            
            if (exists) {
                return SaveForLaterResponse.builder()
                        .success(true)
                        .message("Email này đã được đăng ký")
                        .build();
            }
            
            // Create new notification request
            StockNotificationRequest request = new StockNotificationRequest();
            request.setProductId(productId);
            request.setVariantId(variantId);
            request.setEmail(email);
            request.setNotified(false);
            
            stockNotificationRequestRepository.save(request);
            
            log.info("Stock notification registered for product {} variant {} email {}", productId, variantId, email);
            
            return SaveForLaterResponse.builder()
                    .success(true)
                    .message("Đã đăng ký thông báo thành công")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error registering stock notification", e);
            return SaveForLaterResponse.builder()
                    .success(false)
                    .message("Có lỗi xảy ra, vui lòng thử lại")
                    .build();
        }
    }
    
    public List<StockNotificationRequest> getPendingNotifications(Long productId, Long variantId) {
        if (variantId != null) {
            return stockNotificationRequestRepository.findByProductIdAndVariantIdAndNotifiedFalse(productId, variantId);
        } else {
            return stockNotificationRequestRepository.findByProductIdAndNotifiedFalse(productId);
        }
    }
    
    @Transactional
    public void markAsNotified(List<StockNotificationRequest> requests) {
        requests.forEach(request -> {
            request.setNotified(true);
            stockNotificationRequestRepository.save(request);
        });
    }
}