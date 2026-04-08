package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Cần @Modifying vì đây là thao tác thay đổi dữ liệu (Delete)
    @Modifying
    @Transactional
    void deleteAllByCartId(Long cartId);

    @Modifying // Nên thêm cho cả phương thức xóa bằng Object
    @Transactional
    void deleteAllByCart(Cart cart);
}