package com.codegym.smartphonemanagement.service.admin;

import com.codegym.smartphonemanagement.model.RefundRequest;
import com.codegym.smartphonemanagement.model.RefundStatus;
import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.repository.user.OrderRepository;
import com.codegym.smartphonemanagement.repository.user.RefundRequestRepository;
import com.codegym.smartphonemanagement.service.admin.dto.AdminRefundDTO;
import com.codegym.smartphonemanagement.service.admin.dto.RefundStatsDTO;
import com.codegym.smartphonemanagement.service.loyalty.LoyaltyPointService;
import com.codegym.smartphonemanagement.service.EmailService;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyPointService loyaltyPointService;
    private final EmailService emailService;

    // ======================== 1. PAGINATION + FILTER BY STATUS ========================

    /**
     * Lấy danh sách refund có phân trang, hỗ trợ lọc theo trạng thái.
     * @param status Trạng thái lọc (null = tất cả)
     * @param page   Trang hiện tại (0-indexed)
     * @param size   Số bản ghi mỗi trang
     */
    public Page<AdminRefundDTO> getRefundsPaginated(RefundStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<RefundRequest> refundPage;
        if (status != null) {
            refundPage = refundRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            refundPage = refundRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return refundPage.map(this::toDTO);
    }

    /**
     * @deprecated Giữ lại cho backward compatibility. Ưu tiên dùng getRefundsPaginated().
     */
    @Deprecated
    public List<AdminRefundDTO> getAllRefunds() {
        return refundRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ======================== 2. DATE RANGE STATISTICS ========================

    /**
     * Tính toán thống kê refund theo khoảng thời gian.
     * @param dateFrom Ngày bắt đầu (null = 30 ngày trước)
     * @param dateTo   Ngày kết thúc (null = hôm nay)
     */
    public RefundStatsDTO getRefundStats(LocalDate dateFrom, LocalDate dateTo) {
        // Default: 30 ngày gần nhất
        if (dateTo == null) dateTo = LocalDate.now();
        if (dateFrom == null) dateFrom = dateTo.minusDays(30);

        LocalDateTime from = dateFrom.atStartOfDay();
        LocalDateTime to = dateTo.atTime(LocalTime.MAX);

        long pendingCount = refundRequestRepository.countByStatusAndDateRange(RefundStatus.PENDING, from, to);
        BigDecimal pendingAmt = refundRequestRepository.sumAmountByStatusAndDateRange(RefundStatus.PENDING, from, to);

        long approvedCount = refundRequestRepository.countByStatusAndDateRange(RefundStatus.COMPLETED, from, to);
        BigDecimal approvedAmt = refundRequestRepository.sumAmountByStatusAndDateRange(RefundStatus.COMPLETED, from, to);

        long rejectedCount = refundRequestRepository.countByStatusAndDateRange(RefundStatus.FAILED, from, to);

        return RefundStatsDTO.builder()
                .pendingCount(pendingCount)
                .pendingAmount(pendingAmt != null ? pendingAmt : BigDecimal.ZERO)
                .approvedCount(approvedCount)
                .approvedAmount(approvedAmt != null ? approvedAmt : BigDecimal.ZERO)
                .rejectedCount(rejectedCount)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
    }

    /**
     * Backward compatible: không truyền date range → dùng default 30 ngày.
     */
    public RefundStatsDTO getRefundStats() {
        return getRefundStats(null, null);
    }

    // ======================== 3. APPROVE + EMAIL NOTIFICATION ========================

    @Transactional
    public void approveRefund(Long id, String adminNote) {
        RefundRequest refund = refundRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hoàn ID: " + id));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Đơn này không ở trạng thái chờ duyệt.");
        }

        // Đổi trạng thái
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setAdminNote(adminNote);
        refundRequestRepository.save(refund);

        // Đổi trạng thái Order
        Order order = refund.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
        }

        // Tích tiền vào Ví Loyalty (100đ = 1 điểm)
        BigDecimal amount = refund.getTotalRefundAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            int pointsToAdd = (int) (amount.doubleValue() / 100.0);
            if (pointsToAdd > 0) {
                String reason = "Hoàn tiền cho đơn hàng #" + (order != null ? order.getId() : "N/A");
                loyaltyPointService.adminAdjustPoints(refund.getUser().getId(), pointsToAdd, reason);
                log.info("Cộng {} điểm cho User ID: {} từ khoản hoàn {}", pointsToAdd, refund.getUser().getId(), amount);
            }
        }

        // 🔔 GỬI EMAIL THÔNG BÁO DUYỆT
        sendRefundEmail(refund, true, adminNote);
    }

    // ======================== 4. REJECT + EMAIL NOTIFICATION ========================

    @Transactional
    public void rejectRefund(Long id, String adminNote) {
        RefundRequest refund = refundRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hoàn ID: " + id));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Đơn này không ở trạng thái chờ duyệt.");
        }

        refund.setStatus(RefundStatus.FAILED);
        refund.setAdminNote(adminNote);
        refundRequestRepository.save(refund);

        // 🔔 GỬI EMAIL THÔNG BÁO TỪ CHỐI
        sendRefundEmail(refund, false, adminNote);
    }

    // ======================== EMAIL HELPER ========================

    /**
     * Gửi email thông báo kết quả xử lý refund cho khách hàng.
     * Sử dụng @Async từ EmailService để không block transaction.
     */
    private void sendRefundEmail(RefundRequest refund, boolean approved, String adminNote) {
        try {
            if (refund.getUser() == null || refund.getUser().getEmail() == null) {
                log.warn("Không gửi được email refund: user hoặc email null cho refund #{}", refund.getId());
                return;
            }

            String userEmail = refund.getUser().getEmail();
            String userName = refund.getUser().getUsername();
            Long orderId = refund.getOrder() != null ? refund.getOrder().getId() : null;
            String amountFormatted = refund.getTotalRefundAmount() != null
                    ? String.format("%,.0f", refund.getTotalRefundAmount()) + " ₫"
                    : "N/A";

            if (approved) {
                emailService.sendRefundApprovedEmail(userEmail, userName, orderId, amountFormatted, adminNote);
            } else {
                emailService.sendRefundRejectedEmail(userEmail, userName, orderId, amountFormatted, adminNote);
            }

            log.info("Đã gửi email refund {} cho {} (refund #{})", approved ? "APPROVED" : "REJECTED", userEmail, refund.getId());
        } catch (Exception e) {
            // Không throw — email failure không nên rollback transaction
            log.error("Lỗi gửi email refund cho refund #{}: {}", refund.getId(), e.getMessage());
        }
    }

    // ======================== DTO MAPPER ========================

    private AdminRefundDTO toDTO(RefundRequest r) {
        return AdminRefundDTO.builder()
                .id(r.getId())
                .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
                .username(r.getUser() != null ? r.getUser().getUsername() : "N/A")
                .userEmail(r.getUser() != null ? r.getUser().getEmail() : "N/A")
                .reason(r.getReason())
                .totalRefundAmount(r.getTotalRefundAmount())
                .status(r.getStatus().name())
                .adminNote(r.getAdminNote())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
