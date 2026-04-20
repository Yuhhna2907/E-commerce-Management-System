package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.RefundRequest;
import com.codegym.smartphonemanagement.model.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByOrderId(Long orderId);
    List<RefundRequest> findByUserId(Long userId);
    List<RefundRequest> findByStatus(RefundStatus status);
    List<RefundRequest> findAllByOrderByCreatedAtDesc();
    Optional<RefundRequest> findByOrderIdAndStatus(Long orderId, RefundStatus status);
    boolean existsByOrderIdAndStatus(Long orderId, RefundStatus status);

    // --- PAGINATION ---
    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<RefundRequest> findByStatusOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

    // --- DATE RANGE STATISTICS ---
    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE r.status = :status AND r.createdAt BETWEEN :from AND :to")
    long countByStatusAndDateRange(@Param("status") RefundStatus status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(r.totalRefundAmount), 0) FROM RefundRequest r WHERE r.status = :status AND r.createdAt BETWEEN :from AND :to")
    BigDecimal sumAmountByStatusAndDateRange(@Param("status") RefundStatus status,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    // --- DASHBOARD METRICS ---
    @Query("SELECT COALESCE(SUM(r.totalRefundAmount), 0) FROM RefundRequest r WHERE r.status = 'PENDING'")
    BigDecimal calculateTotalPendingRefundAmount();
}
