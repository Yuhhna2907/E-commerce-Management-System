package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProductAndProductVariant(Cart cart, Product product, ProductVariant productVariant);

    // Cần @Modifying vì đây là thao tác thay đổi dữ liệu (Delete)
    @Modifying
    @Transactional
    void deleteAllByCartId(Long cartId);

    @Modifying // Nên thêm cho cả phương thức xóa bằng Object
    @Transactional
    void deleteAllByCart(Cart cart);

    // FIX #5: Đếm số lượng cart items có product này
    long countByProductId(Long productId);
}