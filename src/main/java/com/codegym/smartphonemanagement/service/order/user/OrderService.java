package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.cart.DTO.CartItemRequestDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
import com.codegym.smartphonemanagement.service.order.DTO.*;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.UnauthorizedAccessException;
import com.codegym.smartphonemanagement.exception.InsufficientStockException;
import com.codegym.smartphonemanagement.exception.InvalidOrderStatusException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codegym.smartphonemanagement.service.NotificationService;
import com.codegym.smartphonemanagement.service.loyalty.ILoyaltyPointService;
import com.codegym.smartphonemanagement.service.recommendation.RecommendationService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final RecommendationService recommendationService;
    private final ILoyaltyPointService loyaltyPointService;

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
                .orElseThrow(() -> new EntityNotFoundException("Giỏ hàng không tồn tại!"));
        if (cart.getItems().isEmpty()) throw new BadRequestException("Giỏ hàng trống!");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại!"));

        String fullAddress = String.format("%s, %s, %s, %s",
                orderDTO.getAddressDetail(),
                orderDTO.getWard(),
                orderDTO.getDistrict(),
                orderDTO.getProvince());

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính phí giao hàng (30k cho HN/HCM/ĐN, 50k cho tỉnh khác)
        BigDecimal shippingFee = BigDecimal.valueOf(50000);
        String lowerProv = orderDTO.getProvince() != null ? orderDTO.getProvince().toLowerCase() : "";
        if (lowerProv.contains("hà nội") || lowerProv.contains("hồ chí minh") || lowerProv.contains("đà nẵng")) {
            shippingFee = BigDecimal.valueOf(30000);
        }

        Order order = Order.builder()
                .user(user)
                .receiverName(orderDTO.getCustomerName())
                .receiverPhone(orderDTO.getReceiverPhone())
                .shippingAddress(fullAddress)
                .note(orderDTO.getNote())
                .totalPrice(total.add(shippingFee)) // Cập nhật có phí vận chuyển
                .shippingFee(shippingFee)
                .status(OrderStatus.PENDING)
                .paymentMethod(orderDTO.getPaymentMethod())
                .couponCode(orderDTO.getCouponCode())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Chuyển CartItem → OrderItem & TRỪ KHO
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            ProductVariant variant = cartItem.getProductVariant();
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Sản phẩm " + variant.getVariantName() + " không đủ hàng!");
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

        // Cập nhật totalPrice = original + shippingFee - discount
        if (savedOrder.getTotalDiscount() != null && savedOrder.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal finalPrice = total.add(savedOrder.getShippingFee()).subtract(savedOrder.getTotalDiscount());
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) finalPrice = BigDecimal.ZERO;
            savedOrder.setTotalPrice(finalPrice);
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
                .orElseThrow(() -> new EntityNotFoundException("Đơn hàng không tồn tại!"));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác đơn hàng này.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Chỉ có thể hủy đơn khi đang ở trạng thái Chờ xác nhận.");
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
                .orElseThrow(() -> new EntityNotFoundException("Đơn hàng không tồn tại!"));

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
        notificationService.sendNotification(order.getUser(), msg, NotificationType.ORDER_STATUS_CHANGED, "/user/order/detail/" + order.getId());

        // Update Recommendations Real-time
        if (newStatus == OrderStatus.DELIVERED && order.getItems() != null) {
            order.getItems().forEach(item -> {
                try {
                    recommendationService.buildRecommendationsForProduct(item.getProduct().getId());
                    log.info("Successfully triggered real-time recommendation build for product: {}", item.getProduct().getId());
                } catch (Exception e) {
                    log.error("Failed to build real-time recommendation for product: {}", item.getProduct().getId(), e);
                }
            });
        }

        // ── LOYALTY POINTS ──
        if (newStatus == OrderStatus.DELIVERED) {
            try {
                loyaltyPointService.earnPoints(order.getUser().getId(), order);
                int earned = order.getPointsEarned() != null ? order.getPointsEarned() : 0;
                if (earned > 0) {
                    String pointMsg = String.format(
                            "🌟 Bạn vừa tích được %d điểm từ đơn hàng #%d! Dùng điểm để đổi ưu đãi tại trang Điểm Tích Lũy.",
                            earned, order.getId());
                    notificationService.sendNotification(
                            order.getUser(), pointMsg,
                            NotificationType.ORDER_STATUS_CHANGED,
                            "/user/loyalty");
                }
                orderRepository.save(order); // Lưu pointsEarned vào DB
            } catch (Exception e) {
                log.error("Failed to earn loyalty points for order #{}", order.getId(), e);
            }
        }

        if (newStatus == OrderStatus.REFUNDED || newStatus == OrderStatus.PARTIAL_REFUNDED) {
            try {
                loyaltyPointService.deductPoints(order.getUser().getId(), order);
            } catch (Exception e) {
                log.error("Failed to deduct loyalty points for order #{}", order.getId(), e);
            }
        }
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
                .orElseThrow(() -> new EntityNotFoundException("Đơn hàng không tồn tại!"));
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền xem đơn hàng này.");
        }
        return mapToResponseDTO(order);
    }

    @Override
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng!"));
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
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng!"));
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác đơn hàng này.");
        }
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Đơn hàng không có sản phẩm.");
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
            throw new InvalidOrderStatusException(
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

        // Calculate estimated delivery date (3-5 business days from order creation)
        LocalDateTime estimatedDelivery = calculateEstimatedDeliveryDate(order.getCreatedAt(), 5);

        return OrderResponseDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .totalDiscount(order.getTotalDiscount())
                .shippingFee(order.getShippingFee())
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
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING")
                .paymentStatusDisplay(getPaymentStatusDisplay(order.getPaymentStatus()))
                .items(itemDTOs)
                .timeline(timeline)
                .activeRefund(activeRefund)
                .canCancel(canCancel)
                .canRefund(canRefund)
                .estimatedDeliveryDate(estimatedDelivery)
                // Thông tin người mua
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .buyerUsername(order.getUser() != null ? order.getUser().getUsername() : "")
                .buyerFullName(order.getUser() != null ? order.getUser().getFullName() : "")
                .build();
    }

    private String getPaymentStatusDisplay(com.codegym.smartphonemanagement.model.PaymentStatus status) {
        if (status == null) return "Chờ thanh toán";
        switch (status) {
            case COMPLETED: return "Đã thanh toán";
            case FAILED: return "Thanh toán thất bại";
            case REFUNDED: return "Đã hoàn tiền";
            case PENDING:
            default: return "Chờ thanh toán";
        }
    }

    /**
     * Calculate estimated delivery date by adding business days (skip weekends)
     * @param startDate The order creation date
     * @param businessDays Number of business days to add (e.g., 5 for 5 business days)
     * @return Estimated delivery date
     */
    private LocalDateTime calculateEstimatedDeliveryDate(LocalDateTime startDate, int businessDays) {
        LocalDateTime result = startDate;
        int addedDays = 0;
        
        while (addedDays < businessDays) {
            result = result.plusDays(1);
            // Skip weekends (Saturday = 6, Sunday = 7)
            if (result.getDayOfWeek().getValue() < 6) {
                addedDays++;
            }
        }
        
        return result;
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