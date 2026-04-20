package com.codegym.smartphonemanagement.service.wishlist;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.dto.WishlistItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IWishlistService {
    /**
     * Toggles a wishlist item.
     * @return true if added, false if removed
     */
    boolean toggleWishlist(Long userId, Long productId);

    /**
     * Removes an item from the user's wishlist
     */
    void removeWishlistItem(Long userId, Long productId);

    /**
     * Retrieves all products in a user's wishlist (deprecated - use getWishlistItems instead)
     * @deprecated Use {@link #getWishlistItems(Long, Pageable)} instead
     */
    @Deprecated
    List<Product> getWishlistProductsByUserId(Long userId);
    
    /**
     * Retrieves wishlist items with pagination and DTO mapping
     */
    Page<WishlistItemDTO> getWishlistItems(Long userId, Pageable pageable);
    
    /**
     * Checks if a product is in user's wishlist
     */
    boolean isInWishlist(Long userId, Long productId);
    
    /**
     * Counts total wishlist items for a user
     */
    long countWishlistItems(Long userId);
    
    /**
     * Clears all wishlist items for a user
     */
    void clearWishlist(Long userId);
}
