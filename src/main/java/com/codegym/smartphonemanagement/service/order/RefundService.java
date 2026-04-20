package com.codegym.smartphonemanagement.service.order;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.coupon.ICouponService;
import com.codegym.smartphonemanagement.service.order.DTO.*;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.UnauthorizedAccessException;
import com.codegym.smartphonemanagement.exception.InvalidOrderStatusException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codegym.smartphonemanagement.service.NotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("orderRefundService")
@RequiredArgsConstructor
public class RefundService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final RefundItemRepository refundItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ICouponService couponService;
    private final NotificationService notificationService;

    private static final int REFUND_WINDOW_DAYS = 14;

    // ======================== USER: TẠO YÊU CẦU REFUND ========================

    @Transactional
    public RefundResponseDTO createRefundRequest(Long userId, RefundRequestDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Đơn hàng không tồn tại!"));

        // Validate ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Bạn không có quyền thao tác đơn hàng này.");
        }

        // Validate status = DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Chỉ có thể yêu cầu hoàn trả khi đơn hàng đã được giao.");
        }

        // Validate within 14-day window
        Optional<OrderHistory> deliveredEvent = orderHistoryRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(h -> h.getStatusTo() == OrderStatus.DELIVERED)
                .findFirst();
        
        if (!deliveredEvent.isPresent()) {
            throw new BadRequestException("Không tìm thấy thông tin giao hàng. Vui lòng liên hệ hỗ trợ.");
        }
        
        LocalDateTime deliveredAt = deliveredEvent.get().getCreatedAt();
        if (deliveredAt.plusDays(REFUND_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Đã quá thời hạn " + REFUND_WINDOW_DAYS + " ngày để yêu cầu hoàn trả.");
        }

        // Validate no pending refund exists
        if (refundRequestRepository.existsByOrderIdAndStatus(order.getId(), RefundStatus.PENDING)) {
            throw new BadRequestException("Đơn hàng này đang có yêu cầu hoàn trả chờ duyệt.");
        }

        // Validate at least 1 item
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 sản phẩm cần hoàn trả.");
        }

        // Filter out items with quantity = 0
        List<RefundItemDTO> validItems = dto.getItems().stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập số lượng trả lại cho ít nhất 1 sản phẩm.");
        }

        // Create RefundRequest
        RefundRequest refundRequest = RefundRequest.builder()
                .order(order)
                .user(order.getUser())
                .reason(dto.getReason())
                .totalRefundAmount(BigDecimal.ZERO)
                .status(RefundStatus.PENDING)
                .build();
        refundRequest = refundRequestRepository.save(refundRequest);

        // Calculate refund per item
        BigDecimal totalRefund = BigDecimal.ZERO;
        List<RefundItem> refundItems = new ArrayList<>();

        for (RefundItemDTO itemDto : validItems) {
            OrderItem orderItem = orderItemRepository.findById(itemDto.getOrderItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại trong đơn hàng."));

            // Validate item belongs to this order
            if (!orderItem.getOrder().getId().equals(order.getId())) {
                throw new BadRequestException("Sản phẩm không thuộc đơn hàng này.");
            }

            // Calculate already refunded quantity for this item
            int alreadyRefundedQty = refundRequestRepository.findByOrderId(order.getId())
                    .stream()
                    .filter(r -> r.getStatus() == RefundStatus.COMPLETED )
                    .flatMap(r -> r.getItems().stream())
                    .filter(ri -> ri.getOrderItem().getId().equals(orderItem.getId()))
                    .mapToInt(RefundItem::getQuantity)
                    .sum();

            // Validate quantity: current + already refunded <= original quantity
            int remainingQty = orderItem.getQuantity() - alreadyRefundedQty;
            if (itemDto.getQuantity() > remainingQty) {
                throw new BadRequestException("Số lượng trả lại không hợp lệ cho " + orderItem.getProduct().getName() 
                        + ". Đã mua: " + orderItem.getQuantity() 
                        + ", Đã hoàn trả: " + alreadyRefundedQty 
                        + ", Còn lại: " + remainingQty);
            }

            // ========== CORE REFUND FORMULA ==========
            // allocatedDiscount is total discount for the entire line (price * qty)
            // Per unit discount = allocatedDiscount / quantity
            BigDecimal allocatedTotal = orderItem.getAllocatedDiscount() != null
                    ? orderItem.getAllocatedDiscount() : BigDecimal.ZERO;
            BigDecimal discountPerUnit = allocatedTotal
                    .divide(BigDecimal.valueOf(orderItem.getQuantity()), 4, RoundingMode.HALF_UP);
            BigDecimal refundPerUnit = orderItem.getPrice().subtract(discountPerUnit);
            BigDecimal itemRefund = refundPerUnit.multiply(BigDecimal.valueOf(itemDto.getQuantity()))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal allocatedForReturn = discountPerUnit.multiply(BigDecimal.valueOf(itemDto.getQuantity()))
                    .setScale(0, RoundingMode.HALF_UP);

            // Validate refund amount > 0
            if (itemRefund.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số tiền hoàn trả không hợp lệ cho " + orderItem.getProduct().getName() 
                        + ". Sản phẩm này có thể đã được giảm giá 100%.");
            }

            RefundItem refundItem = RefundItem.builder()
                    .refundRequest(refundRequest)
                    .orderItem(orderItem)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(orderItem.getPrice())
                    .allocatedDiscount(allocatedForReturn)
                    .refundAmount(itemRefund)
                    .build();

            refundItems.add(refundItem);
            totalRefund = totalRefund.add(itemRefund);
        }

        refundItemRepository.saveAll(refundItems);

        // Check if this request refunds all remaining items to include shipping fee
        boolean refundingAllRemaining = true;
        for (OrderItem oi : order.getItems()) {
            int alreadyRefundedQty = refundRequestRepository.findByOrderId(order.getId())
                    .stream()
                    .filter(r -> r.getStatus() == RefundStatus.COMPLETED || r.getStatus() == RefundStatus.PENDING)
                    .flatMap(r -> r.getItems().stream())
                    .filter(ri -> ri.getOrderItem().getId().equals(oi.getId()))
                    .mapToInt(RefundItem::getQuantity)
                    .sum();
            int remainingQty = oi.getQuantity() - alreadyRefundedQty;
            
            Optional<RefundItemDTO> requested = validItems.stream()
                    .filter(i -> i.getOrderItemId().equals(oi.getId()))
                    .findFirst();
            int qtyToRefund = requested.map(RefundItemDTO::getQuantity).orElse(0);
            
            if (qtyToRefund < remainingQty) {
                refundingAllRemaining = false;
                break;
            }
        }
        
        if (refundingAllRemaining && order.getShippingFee() != null && order.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            totalRefund = totalRefund.add(order.getShippingFee());
        }

        // Update total
        refundRequest.setTotalRefundAmount(totalRefund);
        refundRequest.setItems(refundItems);
        refundRequestRepository.save(refundRequest);

        // Update order status
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        orderRepository.save(order);

        saveHistory(order, oldStatus, OrderStatus.REFUND_REQUESTED, "USER",
                "Khách yêu cầu hoàn trả: " + dto.getReason());

        return mapToDTO(refundRequest);
    }

    // ======================== ADMIN: DUYỆT REFUND ========================

    @Transactional
    public RefundResponseDTO approveRefund(Long refundRequestId, String adminNote) {
        RefundRequest req = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Yêu cầu hoàn trả không tồn tại!"));

        if (req.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Yêu cầu hoàn trả này đã được xử lý.");
        }

        req.setStatus(RefundStatus.COMPLETED);
        req.setAdminNote(adminNote);
        refundRequestRepository.save(req);

        Order order = req.getOrder();

        // Hoàn kho cho từng item
        for (RefundItem ri : req.getItems()) {
            ProductVariant variant = ri.getOrderItem().getProductVariant();
            variant.setStockQuantity(variant.getStockQuantity() + ri.getQuantity());
            productVariantRepository.save(variant);
        }

        // Xác định REFUNDED hay PARTIAL_REFUNDED
        boolean allItemsFullyRefunded = isAllItemsFullyRefunded(order, req);

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;
        boolean shouldRestoreVoucher = false;

        if (allItemsFullyRefunded) {
            newStatus = OrderStatus.REFUNDED;
            // Chỉ hoàn voucher nếu chưa từng hoàn trước đó
            if (order.getCouponCode() != null && !order.getCouponCode().isEmpty()) {
                // Check xem đã hoàn voucher chưa bằng cách kiểm tra history
                boolean voucherAlreadyRestored = orderHistoryRepository
                        .findByOrderIdOrderByCreatedAtDesc(order.getId())
                        .stream()
                        .anyMatch(h -> h.getStatusTo() == OrderStatus.REFUNDED 
                                && h.getReason() != null 
                                && h.getReason().contains("hoàn voucher"));
                
                if (!voucherAlreadyRestored) {
                    shouldRestoreVoucher = true;
                }
            }
        } else {
            newStatus = OrderStatus.PARTIAL_REFUNDED;
        }

        // Cập nhật totalPrice của order (trừ đi số tiền refund)
        BigDecimal currentTotal = order.getTotalPrice();
        BigDecimal newTotal = currentTotal.subtract(req.getTotalRefundAmount());
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }
        order.setTotalPrice(newTotal);
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Hoàn voucher nếu cần
        if (shouldRestoreVoucher) {
            couponService.restoreVoucherUsage(order.getCouponCode());
            saveHistory(order, oldStatus, newStatus, "ADMIN",
                    "Admin duyệt hoàn trả (đã hoàn voucher)" + (adminNote != null ? ": " + adminNote : ""));
        } else {
            saveHistory(order, oldStatus, newStatus, "ADMIN",
                    "Admin duyệt hoàn trả" + (adminNote != null ? ": " + adminNote : ""));
        }
        
        // Notify user
        String msg = String.format("Yêu cầu hoàn trả cho đơn hàng #%d đã được duyệt. Trạng thái đơn: %s", 
                order.getId(), newStatus.name());
        notificationService.sendNotification(order.getUser(), msg, NotificationType.ORDER_STATUS_CHANGED, "/user/order/detail/" + order.getId());

        return mapToDTO(req);
    }

    // ======================== ADMIN: TỪ CHỐI REFUND ========================

    @Transactional
    public RefundResponseDTO rejectRefund(Long refundRequestId, String adminNote) {
        RefundRequest req = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Yêu cầu hoàn trả không tồn tại!"));

        if (req.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Yêu cầu hoàn trả này đã được xử lý.");
        }

        req.setStatus(RefundStatus.FAILED);
        req.setAdminNote(adminNote);
        refundRequestRepository.save(req);

        // Đặt lại order về DELIVERED
        Order order = req.getOrder();
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        saveHistory(order, oldStatus, OrderStatus.DELIVERED, "ADMIN",
                "Admin từ chối hoàn trả" + (adminNote != null ? ": " + adminNote : ""));
                
        // Notify user
        String msg = String.format("Yêu cầu hoàn trả cho đơn hàng #%d đã bị từ chối.", order.getId());
        notificationService.sendNotification(order.getUser(), msg, NotificationType.ORDER_STATUS_CHANGED, "/user/order/detail/" + order.getId());

        return mapToDTO(req);
    }

    // ======================== QUERIES ========================

    public List<RefundResponseDTO> getAllRefundRequests() {
        return refundRequestRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RefundResponseDTO> getRefundsByStatus(RefundStatus status) {
        return refundRequestRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public RefundResponseDTO getRefundById(Long id) {
        RefundRequest req = refundRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Yêu cầu hoàn trả không tồn tại!"));
        return mapToDTO(req);
    }

    // ======================== HELPERS ========================

    /**
     * Kiểm tra tất cả items trong đơn hàng đã được refund hết chưa
     */
    private boolean isAllItemsFullyRefunded(Order order, RefundRequest currentReq) {
        // Lấy tất cả approved refunds + current request
        List<RefundRequest> approvedReqs = refundRequestRepository.findByOrderId(order.getId())
                .stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED || r.getId().equals(currentReq.getId()))
                .collect(Collectors.toList());

        // Tổng hợp quantity đã refund cho mỗi orderItem
        for (OrderItem oi : order.getItems()) {
            int totalRefundedQty = 0;
            for (RefundRequest rr : approvedReqs) {
                if (rr.getItems() != null) {
                    totalRefundedQty += rr.getItems().stream()
                            .filter(ri -> ri.getOrderItem().getId().equals(oi.getId()))
                            .mapToInt(RefundItem::getQuantity)
                            .sum();
                }
            }
            if (totalRefundedQty < oi.getQuantity()) {
                return false; // Còn item chưa refund hết
            }
        }
        return true;
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

    private RefundResponseDTO mapToDTO(RefundRequest req) {
        List<RefundItemDetailDTO> items = req.getItems() != null ? req.getItems().stream()
                .map(ri -> {
                    BigDecimal discountPerUnit = ri.getAllocatedDiscount()
                            .divide(BigDecimal.valueOf(ri.getQuantity()), 0, RoundingMode.HALF_UP);
                    return RefundItemDetailDTO.builder()
                            .orderItemId(ri.getOrderItem().getId())
                            .productName(ri.getOrderItem().getProduct().getName())
                            .variantName(ri.getOrderItem().getProductVariant() != null ?
                                    ri.getOrderItem().getProductVariant().getVariantName() : "")
                            .imageUrl(ri.getOrderItem().getProduct().getImageUrl())
                            .quantityOrdered(ri.getOrderItem().getQuantity())
                            .quantityReturned(ri.getQuantity())
                            .unitPrice(ri.getUnitPrice())
                            .allocatedDiscountPerUnit(discountPerUnit)
                            .refundAmount(ri.getRefundAmount())
                            .build();
                })
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
