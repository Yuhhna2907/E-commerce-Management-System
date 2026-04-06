package com.codegym.smartphonemanagement.service.cart.user;

import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;

public interface ICartService {

    CartResponseDTO getCart(Long userId);

    CartResponseDTO addToCart(Long userId, CartItemRequestDTO request);

    CartResponseDTO updateQuantity(Long userId, CartItemRequestDTO request);

    void removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}