package com.codegym.smartphonemanagement.service.savedforlater;

import com.codegym.smartphonemanagement.model.dto.SavedForLaterDTO;
import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductVariant;
import com.codegym.smartphonemanagement.model.entity.SavedForLater;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.ProductVariantRepository;
import com.codegym.smartphonemanagement.repository.user.CartRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.CartItemRepository;
import com.codegym.smartphonemanagement.repository.user.SavedForLaterRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.service.cart.user.CartService;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.UnauthorizedAccessException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedForLaterService {
    
    private final SavedForLaterRepository savedForLaterRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountService discountService;
    private final CartService cartService; // FIX #14: Thêm CartService dependency
    
    /**
     * Lưu cart item để mua sau
     */
    @Transactional
    public SavedForLaterDTO saveForLater(Long userId, Long cartItemId) {
        // Tìm cart item
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item không tồn tại"));
        
        // Kiểm tra quyền sở hữu
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Không có quyền thao tác cart item này");
        }
        
        // Kiểm tra xem đã lưu chưa
        boolean exists = savedForLaterRepository.existsByUserIdAndProductIdAndVariantId(
                userId, 
                cartItem.getProduct().getId(), 
                cartItem.getProductVariant() != null ? cartItem.getProductVariant().getVariantId() : null
        );
        
        if (exists) {
            throw new BadRequestException("Sản phẩm đã được lưu trước đó");
        }
        
        // Tạo saved item
        SavedForLater savedItem = SavedForLater.builder()
                .userId(userId)
                .productId(cartItem.getProduct().getId())
                .variantId(cartItem.getProductVariant() != null ? cartItem.getProductVariant().getVariantId() : null)
                .quantity(cartItem.getQuantity())
                .build();
        
        savedItem = savedForLaterRepository.save(savedItem);
        
        // Xóa khỏi cart
        cartItemRepository.delete(cartItem);
        
        // Convert to DTO
        return convertToDTO(savedItem);
    }
    
    /**
     * Chuyển saved item về giỏ hàng
     */
    // FIX #14: Refactor để sử dụng CartService thay vì tự implement
    @Transactional
    public void moveToCart(Long userId, Long savedItemId) {
        SavedForLater savedItem = savedForLaterRepository.findById(savedItemId)
                .orElseThrow(() -> new EntityNotFoundException("Saved item không tồn tại"));
        
        if (!savedItem.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Không có quyền thao tác saved item này");
        }
        
        // FIX #14: Gọi CartService thay vì tự implement
        // CartService đã có logic check stock + lock đầy đủ
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductId(savedItem.getProductId());
        request.setVariantId(savedItem.getVariantId());
        request.setQuantity(savedItem.getQuantity());
        
        cartService.addToCart(userId, request);
        
        // Xóa saved item sau khi thêm vào cart thành công
        savedForLaterRepository.delete(savedItem);
    }
    
    /**
     * Lấy danh sách saved items của user
     */
    public List<SavedForLaterDTO> getSavedItems(Long userId) {
        List<SavedForLater> savedItems = savedForLaterRepository.findByUserIdOrderBySavedAtDesc(userId);
        List<SavedForLaterDTO> dtos = new ArrayList<>();
        
        for (SavedForLater item : savedItems) {
            dtos.add(convertToDTO(item));
        }
        
        return dtos;
    }
    
    /**
     * Xóa saved item
     */
    @Transactional
    public void removeSavedItem(Long userId, Long savedItemId) {
        SavedForLater savedItem = savedForLaterRepository.findById(savedItemId)
                .orElseThrow(() -> new EntityNotFoundException("Saved item không tồn tại"));
        
        if (!savedItem.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Không có quyền xóa saved item này");
        }
        
        savedForLaterRepository.delete(savedItem);
    }
    
    /**
     * Đếm số lượng saved items
     */
    public long countSavedItems(Long userId) {
        return savedForLaterRepository.countByUserId(userId);
    }
    
    /**
     * Convert entity to DTO
     */
    private SavedForLaterDTO convertToDTO(SavedForLater savedItem) {
        // Lấy product
        Product product = productRepository.findById(savedItem.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại"));
        
        // Lấy variant (nếu có)
        ProductVariant variant = null;
        if (savedItem.getVariantId() != null) {
            variant = productVariantRepository.findById(savedItem.getVariantId()).orElse(null);
        }
        
        // Lấy giá thực tế
        BigDecimal basePrice = variant != null ? variant.getSalePrice() : product.getPrice();
        BigDecimal discountPrice = null;
        if (variant != null) {
            discountPrice = discountService.applyDiscountToVariant(product, basePrice);
        }
        if (discountPrice == null || discountPrice.compareTo(BigDecimal.ZERO) == 0) {
            discountPrice = discountService.applyDiscount(product);
        }
        
        String discountLabel = discountService.getDiscountLabel(product);
        
        return SavedForLaterDTO.builder()
                .id(savedItem.getId())
                .userId(savedItem.getUserId())
                .productId(savedItem.getProductId())
                .variantId(savedItem.getVariantId())
                .quantity(savedItem.getQuantity())
                .savedAt(savedItem.getSavedAt())
                .note(savedItem.getNote())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .price(basePrice)
                .discountPrice(discountPrice)
                .discountLabel(discountLabel)
                .stock(variant != null ? variant.getStockQuantity() : product.getStock())
                .brand(product.getBrand())
                .color(variant != null ? variant.getColor() : null)
                .storage(variant != null ? variant.getStorage() : null)
                .ram(variant != null ? variant.getRam() : null)
                .build();
    }
}
