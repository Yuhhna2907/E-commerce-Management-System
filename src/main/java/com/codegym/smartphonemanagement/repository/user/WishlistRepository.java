package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    int countByUserId(Long userId);
}
