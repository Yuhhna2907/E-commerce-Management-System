package com.codegym.smartphonemanagement.service.cart.user;

import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.CartItemRepository;
import com.codegym.smartphonemanagement.repository.CartRepository;
import com.codegym.smartphonemanagement.repository.ProductRepository;
import com.codegym.smartphonemanagement.repository.UserRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // Lấy hoặc tạo cart
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(cart);
                });
    }

    @Override
    public CartResponseDTO getCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Cart cart = getOrCreateCart(user);

        return mapToResponse(cart);
    }

    @Override
    public CartResponseDTO addToCart(Long userId, CartItemRequestDTO request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
        }

        cartItemRepository.save(item);

        return mapToResponse(cart);
    }

    @Override
    public CartResponseDTO updateQuantity(Long userId, CartItemRequestDTO request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("Item không tồn tại"));

        item.setQuantity(request.getQuantity());

        cartItemRepository.save(item);

        return mapToResponse(cart);
    }

    @Override
    public void removeItem(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("Item không tồn tại"));

        cartItemRepository.delete(item);
    }

    @Override
    public void clearCart(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Cart cart = getOrCreateCart(user);

        cartItemRepository.deleteAll(cart.getItems());
    }

    // ================= MAP =================
    private CartResponseDTO mapToResponse(Cart cart) {

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (cart.getItems() == null) {
            return CartResponseDTO.builder()
                    .cartId(cart.getId())
                    .items(itemDTOs)
                    .totalPrice(total)
                    .build();
        }

        for (CartItem item : cart.getItems()) {

            BigDecimal itemTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            total = total.add(itemTotal);

            itemDTOs.add(
                    CartItemResponseDTO.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .price(item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .total(itemTotal)
                            .build()
            );
        }

        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .items(itemDTOs)
                .totalPrice(total)
                .build();
    }
}