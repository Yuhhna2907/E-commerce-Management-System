package com.codegym.smartphonemanagement.service.cart.user;

import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;

public interface ICartService {

    // Lấy thông tin giỏ hàng hiện tại của User
    CartResponseDTO getCart(Long userId);

    // Thêm sản phẩm vào giỏ
    CartResponseDTO addToCart(Long userId, CartItemRequestDTO request);

    // Cập nhật số lượng (Tăng/Giảm)
    CartResponseDTO updateQuantity(Long userId, CartItemRequestDTO request);

    // Xóa một sản phẩm cụ thể khỏi giỏ
    void removeItem(Long userId, Long productId);

    // Xóa sạch giỏ hàng (thường dùng sau khi đặt hàng thành công)
    void clearCart(Long userId);

    // Bỏ hàm getCartByUserId nếu nó trùng lặp với getCart

    CartResponseDTO getCartByUserId(Long userId);
}