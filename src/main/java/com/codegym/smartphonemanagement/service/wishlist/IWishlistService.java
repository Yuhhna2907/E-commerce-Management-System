package com.codegym.smartphonemanagement.service.wishlist;

import com.codegym.smartphonemanagement.model.Product;
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
     * Retrieves all products in a user's wishlist
     */
    List<Product> getWishlistProductsByUserId(Long userId);
}
