package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
import com.codegym.smartphonemanagement.service.order.DTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codegym.smartphonemanagement.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private final OrderHistoryRepository orderHistoryRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final ICartService cartService;
    private final ICouponService couponService;
    private final NotificationService notificationService;

    // Giả lập (thay bằng Security context sau)
    private static final Long MOCK_USER_ID = 1L;

    // ======================= STATE MACHINE CONFIG =======================

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUND_REQUESTED),
            OrderStatus.REFUND_REQUESTED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PARTIAL_REFUNDED, OrderStatus.DELIVERED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class), // Terminal state
            OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class), // Terminal state
            OrderStatus.PARTIAL_REFUNDED, EnumSet.of(OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED) // Allow additional refunds
    );

    private static final Map<OrderStatus, String> STATUS_DISPLAY = Map.of(
            OrderStatus.PENDING, "Chờ xác nhận",
            OrderStatus.CONFIRMED, "Đã xác nhận",
            OrderStatus.SHIPPING, "Đang vận chuyển",
            OrderStatus.DELIVERED, "Đã giao hàng",
            OrderStatus.CANCELLED, "Đã hủy",
            OrderStatus.REFUND_REQUESTED, "Yêu cầu hoàn trả",
            OrderStatus.REFUNDED, "Đã hoàn trả",
            OrderStatus.PARTIAL_REFUNDED, "Hoàn trả một phần"
    );

    // ======================== USER: TẠO ĐƠN ========================

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderDTO) {
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

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .receiverName(orderDTO.getCustomerName())
                .receiverPhone(orderDTO.getReceiverPhone())
                .shippingAddress(fullAddress)
                .note(orderDTO.getNote())
                .totalPrice(total)
                .status(OrderStatus.PENDING)
                .paymentMethod(orderDTO.getPaymentMethod())
                .couponCode(orderDTO.getCouponCode())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Chuyển CartItem → OrderItem & TRỪ KHO
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            ProductVariant variant = cartItem.getProductVariant();
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + variant.getVariantName() + " không đủ hàng!");
            }
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
        savedOrder.setItems(orderItems);

        // Áp dụng Voucher (phân bổ discount vào từng OrderItem)
        couponService.applyDiscountToOrder(savedOrder);
        orderItemRepository.saveAll(orderItems); // Save allocated discounts

        // Cập nhật totalPrice = original - discount
        if (savedOrder.getTotalDiscount() != null && savedOrder.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            savedOrder.setTotalPrice(total.subtract(savedOrder.getTotalDiscount()));
            orderRepository.save(savedOrder);
        }

        // Xóa giỏ hàng
        cartItemRepository.deleteAllByCartId(cart.getId());

        // Ghi audit trail
        saveHistory(savedOrder, null, OrderStatus.PENDING, "SYSTEM", "Đơn hàng được tạo");

        return mapToResponseDTO(savedOrder);
    }

    // ======================== USER: HỦY ĐƠN ========================

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang ở trạng thái Chờ xác nhận.");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Hoàn kho
        restoreStock(order);
        // Hoàn voucher usage
        couponService.restoreVoucherUsage(order.getCouponCode());
        // Ghi audit
        saveHistory(order, oldStatus, OrderStatus.CANCELLED, "USER", "Khách hàng hủy đơn");
    }

    // ======================== ADMIN: CHUYỂN TRẠNG THÁI ========================

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        OrderStatus oldStatus = order.getStatus();
        validateTransition(oldStatus, newStatus);

        // Nếu Admin hủy đơn → hoàn kho + voucher
        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
            couponService.restoreVoucherUsage(order.getCouponCode());
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        saveHistory(order, oldStatus, newStatus, "ADMIN",
                reason != null ? reason : "Admin cập nhật trạng thái");
                
        // Send Notification
        String msg = String.format("Đơn hàng #%d của bạn đã chuyển sang trạng thái: %s", 
             order.getId(), STATUS_DISPLAY.getOrDefault(newStatus, newStatus.name()));
        notificationService.sendNotification(order.getUser(), msg, NotificationType.ORDER_STATUS_CHANGED, "/user/orders/" + order.getId());
    }

    // Backward compat — old signature
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        updateOrderStatus(orderId, newStatus, null);
    }

    // ======================== ADMIN: LIST / FILTER ========================

    public List<OrderResponseDTO> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAllByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    // ======================== USER: XEM CHI TIẾT ========================

    @Override
    public OrderResponseDTO getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này.");
        }
        return mapToResponseDTO(order);
    }

    @Override
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        return mapToResponseDTO(order);
    }

    // ======================== USER: LỊCH SỬ ========================

    @Override
    public List<OrderResponseDTO> getOrderHistory(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    // ======================== USER: ĐẶT LẠI ========================

    @Override
    @Transactional
    public void reorderOrderToCart(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này.");
        }
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm.");
        }
        for (OrderItem oi : items) {
            CartItemRequestDTO req = new CartItemRequestDTO();
            req.setProductId(oi.getProduct().getId());
            req.setVariantId(oi.getProductVariant().getVariantId());
            req.setQuantity(oi.getQuantity());
            cartService.addToCart(userId, req);
        }
    }

    @Override
    public long countInProgressOrdersByUser(Long userId) {
        return orderRepository.countByUser_IdAndStatusIn(
                userId,
                EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING)
        );
    }

    // ======================== HELPERS ========================

    private void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new RuntimeException(
                    String.format("Không thể chuyển trạng thái từ %s sang %s", from, to));
        }
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }
    }

    private void saveHistory(Order order, OrderStatus from, OrderStatus to, String updatedBy, String reason) {
        OrderHistory history = OrderHistory.builder()
                .order(order)
                .statusFrom(from)
                .statusTo(to)
                .updatedBy(updatedBy)
                .reason(reason)
                .build();
        orderHistoryRepository.save(history);
    }

    // ======================== MAPPING DTO ========================

    private OrderResponseDTO mapToResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(item -> {
                    BigDecimal subTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal allocated = item.getAllocatedDiscount() != null ? item.getAllocatedDiscount() : BigDecimal.ZERO;
                    return OrderItemResponseDTO.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .variantId(item.getProductVariant() != null ? item.getProductVariant().getVariantId() : null)
                            .productName(item.getProduct().getName())
                            .variantName(item.getProductVariant() != null ? item.getProductVariant().getVariantName() : "")
                            .imageUrl(item.getProduct().getImageUrl())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .subTotal(subTotal)
                            .allocatedDiscount(allocated)
                            .netPrice(subTotal.subtract(allocated))
                            .build();
                })
                .collect(Collectors.toList());

        // Timeline
        List<OrderHistoryDTO> timeline = orderHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(h -> OrderHistoryDTO.builder()
                        .statusFrom(h.getStatusFrom() != null ? h.getStatusFrom().name() : null)
                        .statusTo(h.getStatusTo().name())
                        .updatedBy(h.getUpdatedBy())
                        .reason(h.getReason())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Active refund
        RefundResponseDTO activeRefund = null;
        RefundRequest refReq = refundRequestRepository
                .findByOrderIdAndStatus(order.getId(), RefundStatus.PENDING)
                .orElse(null);
        if (refReq != null) {
            activeRefund = mapRefundToDTO(refReq);
        }

        // Can cancel / can refund flags
        boolean canCancel = order.getStatus() == OrderStatus.PENDING;

        // refund trong 14 ngày sau DELIVERED
        boolean canRefund = false;
        if (order.getStatus() == OrderStatus.DELIVERED) {
            // Tìm thời điểm chuyển sang DELIVERED
            Optional<OrderHistory> deliveredEvent = orderHistoryRepository
                    .findByOrderIdOrderByCreatedAtDesc(order.getId())
                    .stream()
                    .filter(h -> h.getStatusTo() == OrderStatus.DELIVERED)
                    .findFirst();
            if (deliveredEvent.isPresent()) {
                LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
                canRefund = deliveredAt.plusDays(14).isAfter(LocalDateTime.now());
            }
            // Nếu không tìm thấy event, không cho refund (an toàn hơn)
        }

        String statusName = order.getStatus() != null ? order.getStatus().name() : "PENDING";

        return OrderResponseDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .totalDiscount(order.getTotalDiscount())
                .couponCode(order.getCouponCode())
                .status(statusName)
                .statusDisplay(STATUS_DISPLAY.getOrDefault(order.getStatus(), statusName))
                .createdAt(order.getCreatedAt())
                .customerName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .paymentMethodDisplay(order.getPaymentMethod() != null ?
                        order.getPaymentMethod().getDisplayValue() : "Chưa xác định")
                .items(itemDTOs)
                .timeline(timeline)
                .activeRefund(activeRefund)
                .canCancel(canCancel)
                .canRefund(canRefund)
                .build();
    }

    private RefundResponseDTO mapRefundToDTO(RefundRequest req) {
        List<RefundItemDetailDTO> items = req.getItems() != null ? req.getItems().stream()
                .map(ri -> RefundItemDetailDTO.builder()
                        .orderItemId(ri.getOrderItem().getId())
                        .productName(ri.getOrderItem().getProduct().getName())
                        .variantName(ri.getOrderItem().getProductVariant() != null ?
                                ri.getOrderItem().getProductVariant().getVariantName() : "")
                        .imageUrl(ri.getOrderItem().getProduct().getImageUrl())
                        .quantityOrdered(ri.getOrderItem().getQuantity())
                        .quantityReturned(ri.getQuantity())
                        .unitPrice(ri.getUnitPrice())
                        .allocatedDiscountPerUnit(ri.getAllocatedDiscount()
                                .divide(BigDecimal.valueOf(ri.getQuantity()), 0, java.math.RoundingMode.HALF_UP))
                        .refundAmount(ri.getRefundAmount())
                        .build())
                .collect(Collectors.toList()) : List.of();

        return RefundResponseDTO.builder()
                .id(req.getId())
                .orderId(req.getOrder().getId())
                .reason(req.getReason())
                .totalRefundAmount(req.getTotalRefundAmount())
                .status(req.getStatus().name())
                .adminNote(req.getAdminNote())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .userName(req.getUser() != null ? req.getUser().getUsername() : "")
                .items(items)
                .build();
    }
}