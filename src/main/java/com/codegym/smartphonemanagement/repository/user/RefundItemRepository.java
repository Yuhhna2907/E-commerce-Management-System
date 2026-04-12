package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {
    List<RefundItem> findByRefundRequestId(Long refundRequestId);
}
