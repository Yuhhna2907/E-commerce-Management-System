package com.codegym.smartphonemanagement.service.savedforlater;

import com.codegym.smartphonemanagement.model.dto.SavedForLaterDTO;
import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductVariant;
import com.codegym.smartphonemanagement.model.entity.SavedForLater;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.ProductVariantRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedForLaterService {
    
    private final SavedForLaterRepository savedForLaterRepository;
    private final CartItemRepository cartItemRepository;
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
        
        // Validate quantity
        if (cartItem.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
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
        
        // Convert to DTO (load product và variant để return)
        Product product = cartItem.getProduct();
        ProductVariant variant = cartItem.getProductVariant();
        
        return convertToDTO(savedItem, product, variant);
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
     * FIX: Batch load products và variants để tránh N+1 query
     */
    @Transactional(readOnly = true)
    public List<SavedForLaterDTO> getSavedItems(Long userId) {
        List<SavedForLater> savedItems = savedForLaterRepository.findByUserIdOrderBySavedAtDesc(userId);
        
        if (savedItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Batch load products
        Set<Long> productIds = savedItems.stream()
                .map(SavedForLater::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        // Batch load variants
        Set<Long> variantIds = savedItems.stream()
                .map(SavedForLater::getVariantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ProductVariant> variantMap = variantIds.isEmpty() ? new HashMap<>() :
                productVariantRepository.findAllById(variantIds).stream()
                        .collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));
        
        // Convert to DTOs và filter out deleted products
        return savedItems.stream()
                .map(item -> convertToDTO(item, productMap, variantMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
    @Transactional(readOnly = true)
    public long countSavedItems(Long userId) {
        return savedForLaterRepository.countByUserId(userId);
    }
    
    /**
     * Convert entity to DTO (single item version)
     */
    private SavedForLaterDTO convertToDTO(SavedForLater savedItem, Product product, ProductVariant variant) {
        if (product == null) {
            return null;
        }
        
        // Tính giá
        BigDecimal basePrice = variant != null ? variant.getSalePrice() : product.getPrice();
        BigDecimal discountPrice = calculateDiscountPrice(product, variant, basePrice);
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
    
    /**
     * Convert entity to DTO (batch loading version)
     * Trả về null nếu product hoặc variant đã bị xóa
     */
    private SavedForLaterDTO convertToDTO(SavedForLater savedItem, 
                                          Map<Long, Product> productMap,
                                          Map<Long, ProductVariant> variantMap) {
        // Lấy product từ map
        Product product = productMap.get(savedItem.getProductId());
        if (product == null) {
            // Product đã bị xóa - return null để filter out
            return null;
        }
        
        // Lấy variant từ map (nếu có)
        ProductVariant variant = null;
        if (savedItem.getVariantId() != null) {
            variant = variantMap.get(savedItem.getVariantId());
            if (variant == null) {
                // Variant đã bị xóa - return null để filter out
                return null;
            }
        }
        
        // Tính giá
        BigDecimal basePrice = variant != null ? variant.getSalePrice() : product.getPrice();
        BigDecimal discountPrice = calculateDiscountPrice(product, variant, basePrice);
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
    
    /**
     * Helper method để tính discount price
     */
    private BigDecimal calculateDiscountPrice(Product product, ProductVariant variant, BigDecimal basePrice) {
        if (variant != null) {
            BigDecimal variantDiscount = discountService.applyDiscountToVariant(product, basePrice);
            if (variantDiscount != null && variantDiscount.compareTo(BigDecimal.ZERO) > 0) {
                return variantDiscount;
            }
        }
        
        BigDecimal productDiscount = discountService.applyDiscount(product);
        return productDiscount != null ? productDiscount : BigDecimal.ZERO;
    }
}
