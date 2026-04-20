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

    // ======================= CONSTANTS =======================
    
    private static final BigDecimal STANDARD_SHIPPING_FEE = BigDecimal.valueOf(50000);
    private static final BigDecimal MAJOR_CITY_SHIPPING_FEE = BigDecimal.valueOf(30000);
    private static final int REFUND_WINDOW_DAYS = 14;
    private static final int ESTIMATED_DELIVERY_BUSINESS_DAYS = 5;
    private static final Set<String> MAJOR_CITIES = Set.of("hà nội", "hồ chí minh", "đà nẵng");

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
        // 1. Validate cart
        Cart cart = validateAndGetCart(userId);
        User user = getUserById(userId);

        // 2. Calculate totals
        BigDecimal subtotal = calculateCartSubtotal(cart);
        BigDecimal shippingFee = calculateShippingFee(orderDTO.getProvince());
        
        // 3. Create order
        Order order = buildOrder(user, orderDTO, subtotal, shippingFee);
        Order savedOrder = orderRepository.save(order);

        // 4. Process order items and update stock
        List<OrderItem> orderItems = processOrderItems(cart, savedOrder);
        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // 5. Apply discount
        applyDiscountToOrder(savedOrder);

        // 6. Clear cart
        cartItemRepository.deleteAllByCartId(cart.getId());

        // 7. Record history
        saveHistory(savedOrder, null, OrderStatus.PENDING, "SYSTEM", "Đơn hàng được tạo");

        return mapToResponseDTO(savedOrder);
    }

    private Cart validateAndGetCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Giỏ hàng không tồn tại!"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống!");
        }
        return cart;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại!"));
    }

    private BigDecimal calculateCartSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateShippingFee(String province) {
        if (province == null) return STANDARD_SHIPPING_FEE;
        
        String lowerProvince = province.toLowerCase();
        boolean isMajorCity = MAJOR_CITIES.stream().anyMatch(lowerProvince::contains);
        
        return isMajorCity ? MAJOR_CITY_SHIPPING_FEE : STANDARD_SHIPPING_FEE;
    }

    private Order buildOrder(User user, OrderRequestDTO orderDTO, BigDecimal subtotal, BigDecimal shippingFee) {
        String fullAddress = formatFullAddress(orderDTO);
        BigDecimal totalPrice = subtotal.add(shippingFee);

        return Order.builder()
                .user(user)
                .receiverName(orderDTO.getCustomerName())
                .receiverPhone(orderDTO.getReceiverPhone())
                .shippingAddress(fullAddress)
                .note(orderDTO.getNote())
                .totalPrice(totalPrice)
                .shippingFee(shippingFee)
                .status(OrderStatus.PENDING)
                .paymentMethod(orderDTO.getPaymentMethod())
                .couponCode(orderDTO.getCouponCode())
                .build();
    }

    private String formatFullAddress(OrderRequestDTO orderDTO) {
        return String.format("%s, %s, %s, %s",
                orderDTO.getAddressDetail(),
                orderDTO.getWard(),
                orderDTO.getDistrict(),
                orderDTO.getProvince());
    }

    private List<OrderItem> processOrderItems(Cart cart, Order order) {
        return cart.getItems().stream()
                .map(cartItem -> createOrderItemAndUpdateStock(cartItem, order))
                .collect(Collectors.toList());
    }

    private OrderItem createOrderItemAndUpdateStock(CartItem cartItem, Order order) {
        ProductVariant variant = cartItem.getProductVariant();
        
        // Validate stock
        if (variant.getStockQuantity() < cartItem.getQuantity()) {
            throw new InsufficientStockException(
                    "Sản phẩm " + variant.getVariantName() + " không đủ hàng!");
        }

        // Update stock
        variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
        productVariantRepository.save(variant);

        // Create order item
        return OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .productVariant(variant)
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPriceAtTime())
                .build();
    }

    private void applyDiscountToOrder(Order order) {
        // Apply voucher discount
        couponService.applyDiscountToOrder(order);
        orderItemRepository.saveAll(order.getItems());

        // Recalculate total price
        if (order.getTotalDiscount() != null && order.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal subtotal = calculateOrderSubtotal(order);
            BigDecimal finalPrice = subtotal.add(order.getShippingFee()).subtract(order.getTotalDiscount());
            
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }
            
            order.setTotalPrice(finalPrice);
            orderRepository.save(order);
        }
    }

    private BigDecimal calculateOrderSubtotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ======================== USER: HỦY ĐƠN ========================

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = findOrderById(orderId);
        validateOrderOwnership(order, userId);
        validateOrderCancellable(order);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Restore resources
        restoreStock(order);
        couponService.restoreVoucherUsage(order.getCouponCode());
        
        // Record history
        saveHistory(order, oldStatus, OrderStatus.CANCELLED, "USER", "Khách hàng hủy đơn");
    }

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Đơn hàng không tồn tại!"));
    }

    private void validateOrderOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác đơn hàng này.");
        }
    }

    private void validateOrderCancellable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Chỉ có thể hủy đơn khi đang ở trạng thái Chờ xác nhận.");
        }
    }

    // ======================== ADMIN: CHUYỂN TRẠNG THÁI ========================

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String reason) {
        Order order = findOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();
        
        validateTransition(oldStatus, newStatus);

        // Handle cancellation
        if (newStatus == OrderStatus.CANCELLED) {
            handleOrderCancellation(order);
        }

        // Update status
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Record history
        saveHistory(order, oldStatus, newStatus, "ADMIN",
                reason != null ? reason : "Admin cập nhật trạng thái");

        // Post-status-change actions
        handlePostStatusChangeActions(order, newStatus);
    }

    private void handleOrderCancellation(Order order) {
        restoreStock(order);
        couponService.restoreVoucherUsage(order.getCouponCode());
    }

    private void handlePostStatusChangeActions(Order order, OrderStatus newStatus) {
        // Send notification
        sendOrderStatusNotification(order, newStatus);

        // Handle delivery completion
        if (newStatus == OrderStatus.DELIVERED) {
            handleOrderDelivered(order);
        }

        // Handle refunds
        if (newStatus == OrderStatus.REFUNDED || newStatus == OrderStatus.PARTIAL_REFUNDED) {
            handleOrderRefunded(order);
        }
    }

    private void sendOrderStatusNotification(Order order, OrderStatus newStatus) {
        String message = String.format("Đơn hàng #%d của bạn đã chuyển sang trạng thái: %s",
                order.getId(), STATUS_DISPLAY.getOrDefault(newStatus, newStatus.name()));
        notificationService.sendNotification(
                order.getUser(), message,
                NotificationType.ORDER_STATUS_CHANGED,
                "/user/order/detail/" + order.getId());
    }

    private void handleOrderDelivered(Order order) {
        // Update recommendations
        updateRecommendations(order);
        
        // Award loyalty points
        awardLoyaltyPoints(order);
    }

    private void updateRecommendations(Order order) {
        if (order.getItems() == null) return;
        
        order.getItems().forEach(item -> {
            try {
                recommendationService.buildRecommendationsForProduct(item.getProduct().getId());
                log.info("Successfully triggered real-time recommendation build for product: {}",
                        item.getProduct().getId());
            } catch (Exception e) {
                log.error("Failed to build real-time recommendation for product: {}",
                        item.getProduct().getId(), e);
            }
        });
    }

    private void awardLoyaltyPoints(Order order) {
        try {
            loyaltyPointService.earnPoints(order.getUser().getId(), order);
            int earned = order.getPointsEarned() != null ? order.getPointsEarned() : 0;
            
            if (earned > 0) {
                sendLoyaltyPointsNotification(order, earned);
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("Failed to earn loyalty points for order #{}", order.getId(), e);
        }
    }

    private void sendLoyaltyPointsNotification(Order order, int pointsEarned) {
        String message = String.format(
                "🌟 Bạn vừa tích được %d điểm từ đơn hàng #%d! Dùng điểm để đổi ưu đãi tại trang Điểm Tích Lũy.",
                pointsEarned, order.getId());
        notificationService.sendNotification(
                order.getUser(), message,
                NotificationType.ORDER_STATUS_CHANGED,
                "/user/loyalty");
    }

    private void handleOrderRefunded(Order order) {
        try {
            loyaltyPointService.deductPoints(order.getUser().getId(), order);
        } catch (Exception e) {
            log.error("Failed to deduct loyalty points for order #{}", order.getId(), e);
        }
    }

    // Backward compatibility
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
        Order order = findOrderById(orderId);
        validateOrderOwnership(order, userId);
        return mapToResponseDTO(order);
    }

    @Override
    public OrderResponseDTO getOrderById(Long id) {
        Order order = findOrderById(id);
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
        Order order = findOrderById(orderId);
        validateOrderOwnership(order, userId);
        validateOrderHasItems(order);

        order.getItems().forEach(orderItem -> addOrderItemToCart(userId, orderItem));
    }

    private void validateOrderHasItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("Đơn hàng không có sản phẩm.");
        }
    }

    private void addOrderItemToCart(Long userId, OrderItem orderItem) {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductId(orderItem.getProduct().getId());
        request.setVariantId(orderItem.getProductVariant().getVariantId());
        request.setQuantity(orderItem.getQuantity());
        cartService.addToCart(userId, request);
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
        List<OrderItemResponseDTO> itemDTOs = mapOrderItems(order);
        List<OrderHistoryDTO> timeline = mapOrderHistory(order);
        RefundResponseDTO activeRefund = findActiveRefund(order);
        
        boolean canCancel = order.getStatus() == OrderStatus.PENDING;
        boolean canRefund = canRequestRefund(order);
        LocalDateTime estimatedDelivery = calculateEstimatedDeliveryDate(
                order.getCreatedAt());

        String statusName = order.getStatus() != null ? order.getStatus().name() : "PENDING";

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
                .paymentStatus(order.getPaymentStatus() != null ? 
                        order.getPaymentStatus().name() : "PENDING")
                .paymentStatusDisplay(getPaymentStatusDisplay(order.getPaymentStatus()))
                .items(itemDTOs)
                .timeline(timeline)
                .activeRefund(activeRefund)
                .canCancel(canCancel)
                .canRefund(canRefund)
                .estimatedDeliveryDate(estimatedDelivery)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .buyerUsername(order.getUser() != null ? order.getUser().getUsername() : "")
                .buyerFullName(order.getUser() != null ? order.getUser().getFullName() : "")
                .build();
    }

    private List<OrderItemResponseDTO> mapOrderItems(Order order) {
        return order.getItems().stream()
                .map(this::mapOrderItem)
                .collect(Collectors.toList());
    }

    private OrderItemResponseDTO mapOrderItem(OrderItem item) {
        BigDecimal subTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal allocated = item.getAllocatedDiscount() != null ? 
                item.getAllocatedDiscount() : BigDecimal.ZERO;

        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .variantId(item.getProductVariant() != null ? 
                        item.getProductVariant().getVariantId() : null)
                .productName(item.getProduct().getName())
                .variantName(item.getProductVariant() != null ? 
                        item.getProductVariant().getVariantName() : "")
                .imageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subTotal(subTotal)
                .allocatedDiscount(allocated)
                .netPrice(subTotal.subtract(allocated))
                .build();
    }

    private List<OrderHistoryDTO> mapOrderHistory(Order order) {
        return orderHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(this::mapHistoryToDTO)
                .collect(Collectors.toList());
    }

    private OrderHistoryDTO mapHistoryToDTO(OrderHistory history) {
        return OrderHistoryDTO.builder()
                .statusFrom(history.getStatusFrom() != null ? 
                        history.getStatusFrom().name() : null)
                .statusTo(history.getStatusTo().name())
                .updatedBy(history.getUpdatedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private RefundResponseDTO findActiveRefund(Order order) {
        return refundRequestRepository
                .findByOrderIdAndStatus(order.getId(), RefundStatus.PENDING)
                .map(this::mapRefundToDTO)
                .orElse(null);
    }

    private boolean canRequestRefund(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return false;
        }

        Optional<OrderHistory> deliveredEvent = orderHistoryRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(h -> h.getStatusTo() == OrderStatus.DELIVERED)
                .findFirst();

        if (deliveredEvent.isEmpty()) {
            return false;
        }

        LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
        return deliveredAt.plusDays(REFUND_WINDOW_DAYS).isAfter(LocalDateTime.now());
    }

    private String getPaymentStatusDisplay(com.codegym.smartphonemanagement.model.PaymentStatus status) {
        if (status == null) return "Chờ thanh toán";
        return switch (status) {
            case COMPLETED -> "Đã thanh toán";
            case FAILED -> "Thanh toán thất bại";
            case REFUNDED -> "Đã hoàn tiền";
            default -> "Chờ thanh toán";
        };
    }

    /**
     * Calculate estimated delivery date by adding business days (skip weekends)
     */
    private LocalDateTime calculateEstimatedDeliveryDate(LocalDateTime startDate) {
        LocalDateTime result = startDate;
        int addedDays = 0;

        while (addedDays < OrderService.ESTIMATED_DELIVERY_BUSINESS_DAYS) {
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