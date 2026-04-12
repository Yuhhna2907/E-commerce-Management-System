package com.codegym.smartphonemanagement.service.wishlist;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.Wishlist;
import com.codegym.smartphonemanagement.repository.user.ProductRepositoryUser;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements IWishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepositoryUser productRepository;

    @Override
    @Transactional
    public boolean toggleWishlist(Long userId, Long productId) {
        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // Đã xóa
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
            return true; // Đã thêm
        }
    }

    @Override
    @Transactional
    public void removeWishlistItem(Long userId, Long productId) {
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getWishlistProductsByUserId(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId).stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }
}
