package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}