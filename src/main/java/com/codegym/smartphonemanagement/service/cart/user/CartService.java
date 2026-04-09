package com.codegym.smartphonemanagement.service.cart.user;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.ResourceNotFoundException;
import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.user.CartItemRepository;
import com.codegym.smartphonemanagement.repository.user.CartRepository;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.ProductVariantRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemResponseDTO;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountService discountService;

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private void checkStock(ProductVariant variant, int quantity) {
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException(
                    "Biến thể '" + variant.getVariantName() + "' của sản phẩm '"
                            + variant.getProduct().getName()
                            + "' chỉ còn " + variant.getStockQuantity() + " sản phẩm"
            );
        }
    }

    @Override
    public CartResponseDTO getCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Cart cart = getOrCreateCart(user);
        return mapToResponseDTO(cart);
    }

    @Override
    public CartResponseDTO getCartByUserId(Long userId) {
        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponseDTO addToCart(Long userId, CartItemRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getVariantId().equals(request.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể không tồn tại"));

        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository
                .findByCartAndProductAndProductVariant(cart, product, variant)
                .orElse(null);

        BigDecimal currentPrice = discountService.applyDiscountToVariant(product, variant.getSalePrice());
        if (currentPrice == null) {
            throw new BadRequestException("Không thể tính giá khuyến mãi cho biến thể này!");
        }

        if (item != null) {
            int newQuantity = item.getQuantity() + request.getQuantity();
            checkStock(variant, newQuantity);

            item.setQuantity(newQuantity);

            if (item.getPriceAtTime() == null) {
                item.setPriceAtTime(currentPrice);
            }

            item.setTotalPrice(item.getPriceAtTime().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            checkStock(variant, request.getQuantity());

            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .priceAtTime(currentPrice)
                    .totalPrice(currentPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
        }

        cartItemRepository.save(item);
        return mapToResponseDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO updateQuantity(Long userId, CartItemRequestDTO request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể không tồn tại"));

        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository
                .findByCartAndProductAndProductVariant(cart, product, variant)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));

        checkStock(variant, request.getQuantity());

        item.setQuantity(request.getQuantity());
        item.setTotalPrice(item.getPriceAtTime().multiply(BigDecimal.valueOf(request.getQuantity())));
        cartItemRepository.save(item);

        return mapToResponseDTO(cart);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item không tồn tại"));
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa sản phẩm này");
        }
        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteAllByCart(cart);
    }

    private CartResponseDTO mapToResponseDTO(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .filter(item -> item.getProductVariant() != null)
                .map(item -> {
                    Product product = item.getProduct();
                    ProductVariant variant = item.getProductVariant();

                    BigDecimal originalVariantPrice = variant.getSalePrice();
                    BigDecimal payablePrice = item.getPriceAtTime() != null
                            ? item.getPriceAtTime()
                            : discountService.applyDiscountToVariant(product, originalVariantPrice);

                    BigDecimal itemTotal = payablePrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                    return CartItemResponseDTO.builder()
                            .id(item.getId())
                            .productId(product.getId())
                            .variantId(variant.getVariantId())
                            .productName(product.getName())
                            .variantName(variant.getVariantName())
                            .imageUrl(product.getImageUrl())
                            .price(originalVariantPrice)
                            .discountPrice(payablePrice)
                            .discountLabel(discountService.getDiscountLabel(product))
                            .quantity(item.getQuantity())
                            .subTotal(itemTotal)
                            .brand(product.getBrand())
                            .color(variant.getColor())
                            .stockQuantity(variant.getStockQuantity())
                            .storage(variant.getStorage())
                            .ram(variant.getRam())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = itemDTOs.stream()
                .map(CartItemResponseDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .items(itemDTOs)
                .totalPrice(total)
                .build();
    }
}