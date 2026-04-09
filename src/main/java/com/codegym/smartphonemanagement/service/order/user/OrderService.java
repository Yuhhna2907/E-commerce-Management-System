package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.seller.ProductRepository;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.order.DTO.OrderItemResponseDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderRequestDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    // ======================== CHỨC NĂNG CHO USER ========================

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderDTO) {
        // 1. Lấy và kiểm tra giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));
        if (cart.getItems().isEmpty()) throw new RuntimeException("Giỏ hàng trống!");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        String fullAddress = String.format("%s, %s, %s, %s",
                orderDTO.getAddressDetail(),
                orderDTO.getWard(),
                orderDTO.getDistrict(),
                orderDTO.getProvince());

        // 2. Tính tổng tiền từ giỏ hàng
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Khởi tạo Order từ DTO
        Order order = Order.builder()
                .user(user)
                .receiverName(orderDTO.getCustomerName())
                .receiverPhone(orderDTO.getReceiverPhone())
                .shippingAddress(fullAddress)
                .note(orderDTO.getNote())
                .totalPrice(total)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4. Chuyển CartItem sang OrderItem & TRỪ KHO
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            ProductVariant variant = cartItem.getProductVariant();

            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + variant.getVariantName() + " không đủ hàng!");
            }

            // Trừ kho
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            return OrderItem.builder()
                    .order(savedOrder)
                    .product(cartItem.getProduct())
                    .productVariant(variant)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPriceAtTime())
                    .build();
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // 5. Xóa giỏ hàng sau khi đặt thành công
        cartItemRepository.deleteAllByCartId(cart.getId());

        savedOrder.setItems(orderItems);

        return mapToResponseDTO(savedOrder);
    }

    @Override
    public List<OrderResponseDTO> getOrderHistory(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    // ======================== CHỨC NĂNG CHO ADMIN ========================

    public List<OrderResponseDTO> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAllByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        // Duy có thể thêm logic nếu status là CANCELLED thì hoàn lại kho tại đây
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @Override
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        return mapToResponseDTO(order);
    }

    // ======================== MAPPING DTO ========================

    private OrderResponseDTO mapToResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .productName(item.getProduct().getName())
                        .variantName(item.getProductVariant() != null ? item.getProductVariant().getVariantName() : "")
                        .imageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        // Tính thành tiền từng món
                        .subTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        // 2. Build OrderResponseDTO tổng thể
        return OrderResponseDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus() != null ? order.getStatus().name() : "PENDING")
                .createdAt(order.getCreatedAt())
                .customerName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .items(itemDTOs)
                .build();
    }
}