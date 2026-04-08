package com.codegym.smartphonemanagement.service.cart.user;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.Cart;
import com.codegym.smartphonemanagement.model.CartItem;
import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.CartItemRepository;
import com.codegym.smartphonemanagement.repository.user.CartRepository;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemResponseDTO;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;

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

    private void checkStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new BadRequestException(
                    "Sản phẩm '" + product.getName() + "' chỉ còn " + product.getStock() + " sản phẩm"
            );
        }
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

        BigDecimal currentPrice = product.getPrice();

        if (item != null) {
            int newQuantity = item.getQuantity() + request.getQuantity();
            checkStock(product, newQuantity);
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPriceAtTime(currentPrice);
            item.setTotalPrice(currentPrice.multiply(new BigDecimal(newQuantity)));
        } else {
            checkStock(product, request.getQuantity());
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceAtTime(currentPrice)
                    .totalPrice(currentPrice.multiply(new BigDecimal(request.getQuantity())))
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

        checkStock(product, request.getQuantity());

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


        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item không tồn tại"));

        cart.getItems().remove(item);

        cartRepository.save(cart);
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
            Product product = item.getProduct();
            BigDecimal discountPrice = discountService.applyDiscount(product);
            String discountLabel = discountService.getDiscountLabel(product);
            BigDecimal itemTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            total = total.add(itemTotal);

            itemDTOs.add(
                    CartItemResponseDTO.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .imageUrl(item.getProduct().getImageUrl())
                            .price(item.getProduct().getPrice())
                            .discountPrice(discountPrice)
                            .discountLabel(discountLabel)
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