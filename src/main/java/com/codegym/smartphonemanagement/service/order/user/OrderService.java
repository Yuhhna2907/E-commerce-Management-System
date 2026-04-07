package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.CartRepository;
import com.codegym.smartphonemanagement.repository.OrderRepository;
import com.codegym.smartphonemanagement.repository.UserRepository;
import com.codegym.smartphonemanagement.service.order.DTO.OrderItemResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService{
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponseDTO checkout(Long userId) {
        // 1. Tìm giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        // --- TÍNH TỔNG TIỀN NGAY TẠI ĐÂY ---
        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Khởi tạo Order
        Order order = Order.builder()
                .user(user)
                .totalPrice(totalAmount) // Gán tổng tiền vừa tính vào đây
                .status(OrderStatus.PENDING)
                .build();

        // 3. Chuyển CartItem sang OrderItem & Trừ kho
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ hàng!");
            }
            product.setStock(product.getStock() - cartItem.getQuantity());

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
        }).collect(Collectors.toList());

        order.setItems(orderItems);

        // 4. Lưu và Xóa giỏ
        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        return mapToResponseDTO(savedOrder);
    }

    @Override
    public List<OrderResponseDTO> getOrderHistory(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return mapToResponseDTO(order);
    }

    private OrderResponseDTO mapToResponseDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                // SỬA Ở ĐÂY: User của bạn dùng username chứ không phải name
                .customerName(order.getUser() != null ? order.getUser().getUsername() : "N/A")
                .items(order.getItems().stream().map(item -> OrderItemResponseDTO.builder()
                        .productName(item.getProduct().getName())
                        .imageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subTotal(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                        .build()).toList())
                .build();
    }
}
