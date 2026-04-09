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
    private final DiscountService discountService;

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
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
        return mapToResponseDTO(cart);
    }

    @Override
    public CartResponseDTO getCartByUserId(Long userId) {
        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponseDTO addToCart(Long userId, CartItemRequestDTO request) {
        // 1. Tìm User và Product
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        // 2. Lấy hoặc tạo mới giỏ hàng
        Cart cart = getOrCreateCart(user);

        // 3. Kiểm tra sản phẩm đã có trong giỏ chưa
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);

        // Lấy giá hiện tại của sản phẩm để tính toán
        BigDecimal currentPrice = product.getPrice();
        if (currentPrice == null) {
            throw new BadRequestException("Sản phẩm chưa có giá bán, không thể thêm vào giỏ!");
        }

        if (item != null) {
            // TRƯỜNG HỢP 1: Sản phẩm ĐÃ CÓ trong giỏ -> Tăng số lượng
            int newQuantity = item.getQuantity() + request.getQuantity();
            checkStock(product, newQuantity);

            item.setQuantity(newQuantity);

            // Cần kiểm tra priceAtTime của item cũ (đề phòng dữ liệu rác bị null)
            if (item.getPriceAtTime() == null) {
                item.setPriceAtTime(currentPrice);
            }

            // Tính lại tổng tiền: PriceAtTime * Quantity
            item.setTotalPrice(item.getPriceAtTime().multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            // TRƯỜNG HỢP 2: Sản phẩm CHƯA CÓ trong giỏ -> Tạo mới hoàn toàn
            checkStock(product, request.getQuantity());

            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceAtTime(currentPrice) // Đảm bảo gán giá trị tại đây
                    .totalPrice(currentPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
        }

        // 4. Lưu vào Database
        cartItemRepository.save(item);

        // 5. Trả về thông tin giỏ hàng mới nhất
        return mapToResponseDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDTO updateQuantity(Long userId, CartItemRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));

        checkStock(product, request.getQuantity());

        item.setQuantity(request.getQuantity());
        item.setTotalPrice(item.getPriceAtTime().multiply(BigDecimal.valueOf(request.getQuantity())));
        cartItemRepository.save(item);

        return mapToResponseDTO(cart);
    }

    @Override
    @Transactional
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
    @Transactional
    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteAllByCart(cart);
    }

    private CartResponseDTO mapToResponseDTO(Cart cart) {
        if (cart == null) return null;

        // 1. Dùng Stream để xử lý danh sách item và tính toán discount luôn một thể
        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();

                    // Tính toán giá sau giảm và nhãn giảm giá của bạn
                    BigDecimal discountPrice = discountService.applyDiscount(product);
                    String discountLabel = discountService.getDiscountLabel(product);

                    // Tính tổng tiền cho từng item (Giá sau giảm * số lượng)
                    BigDecimal itemTotal = discountPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                    return CartItemResponseDTO.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .imageUrl(product.getImageUrl())
                            .price(product.getPrice()) // Giá gốc
                            .discountPrice(discountPrice) // Giá sau giảm
                            .discountLabel(discountLabel) // Nhãn -15%, -12%...
                            .quantity(item.getQuantity())
                            .subTotal(itemTotal) // Thành tiền của món này
                            .brand(product.getBrand())
                            .color(product.getColor())
                            .build();
                })
                .collect(Collectors.toList());

        // 2. Tính tổng cộng (Total) của cả giỏ hàng (Kế thừa cách viết gọn của develop)
        BigDecimal total = itemDTOs.stream()
                .map(CartItemResponseDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Trả về DTO cuối cùng
        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .items(itemDTOs)
                .totalPrice(total)
                .build();
    }
}