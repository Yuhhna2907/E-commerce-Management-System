package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}