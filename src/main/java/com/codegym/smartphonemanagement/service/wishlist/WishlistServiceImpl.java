package com.codegym.smartphonemanagement.service.wishlist;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.Wishlist;
import com.codegym.smartphonemanagement.model.dto.WishlistItemDTO;
import com.codegym.smartphonemanagement.repository.user.ProductRepositoryUser;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.WishlistRepository;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements IWishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepositoryUser productRepository;
    private final DiscountService discountService;

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long productId) {
        log.debug("Toggle wishlist - userId: {}, productId: {}", userId, productId);
        
        // Validate inputs
        validateUserId(userId);
        validateProductId(productId);
        
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            log.info("Removed product {} from wishlist of user {}", productId, userId);
            return false; // Đã xóa
        } else {
            // Verify user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User không tồn tại với ID: " + userId));
            
            // Verify product exists
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại với ID: " + productId));
            
            // Check product is active
            if (product.getActive() == null || !product.getActive()) {
                throw new BadRequestException("Không thể thêm sản phẩm đã ngừng bán vào wishlist");
            }
            
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            
            try {
                wishlistRepository.save(wishlist);
                log.info("Added product {} to wishlist of user {}", productId, userId);
                return true; // Đã thêm
            } catch (DataIntegrityViolationException e) {
                // Duplicate - already in wishlist (race condition)
                log.warn("Duplicate wishlist entry detected for userId: {}, productId: {}", userId, productId);
                return false;
            }
        }
    }

    @Override
    @Transactional
    public void removeWishlistItem(Long userId, Long productId) {
        log.debug("Remove wishlist item - userId: {}, productId: {}", userId, productId);
        
        // Validate inputs
        validateUserId(userId);
        validateProductId(productId);
        
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlist -> {
                    wishlistRepository.delete(wishlist);
                    log.info("Removed wishlist item - userId: {}, productId: {}", userId, productId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Deprecated
    public List<Product> getWishlistProductsByUserId(Long userId) {
        log.debug("Get wishlist products (deprecated) - userId: {}", userId);
        validateUserId(userId);
        
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<WishlistItemDTO> getWishlistItems(Long userId, Pageable pageable) {
        log.debug("Get wishlist items with pagination - userId: {}, page: {}, size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate inputs
        validateUserId(userId);
        
        // Use JOIN FETCH to avoid N+1 query
        Page<Wishlist> wishlistPage = wishlistRepository.findByUserIdWithProductOrderByAddedAtDesc(userId, pageable);
        
        return wishlistPage.map(this::convertToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        log.debug("Check if in wishlist - userId: {}, productId: {}", userId, productId);
        
        // Validate inputs
        validateUserId(userId);
        validateProductId(productId);
        
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countWishlistItems(Long userId) {
        log.debug("Count wishlist items - userId: {}", userId);
        
        // Validate inputs
        validateUserId(userId);
        
        return wishlistRepository.countByUserId(userId);
    }
    
    @Override
    @Transactional
    public void clearWishlist(Long userId) {
        log.info("Clear wishlist - userId: {}", userId);
        
        // Validate inputs
        validateUserId(userId);
        
        wishlistRepository.deleteByUserId(userId);
        log.info("Cleared all wishlist items for user {}", userId);
    }
    
    /**
     * Convert Wishlist entity to WishlistItemDTO
     */
    private WishlistItemDTO convertToDTO(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        
        // Calculate discount price
        BigDecimal discountPrice = discountService.applyDiscount(product);
        String discountLabel = discountService.getDiscountLabel(product);
        
        return WishlistItemDTO.builder()
                .wishlistId(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(product.getImageUrl())
                .price(product.getPrice())
                .discountPrice(discountPrice)
                .discountLabel(discountLabel)
                .stock(product.getStock())
                .addedAt(wishlist.getAddedAt())
                .brand(product.getBrand())
                .inStock(product.getStock() != null && product.getStock() > 0)
                .category(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }
    
    /**
     * Validate userId
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("User ID không hợp lệ");
        }
    }
    
    /**
     * Validate productId
     */
    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("Product ID không hợp lệ");
        }
    }
}
