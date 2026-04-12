package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.RefundRequest;
import com.codegym.smartphonemanagement.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
