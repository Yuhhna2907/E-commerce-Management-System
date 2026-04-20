package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for RefundTransaction entity.
 * Provides query methods for retrieving refund transaction records by various criteria.
 */
@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

    /**
     * Find refund transaction by refund request ID
     * 
     * @param refundRequestId the refund request ID to search for
     * @return Optional containing the refund transaction if found
     */
    Optional<RefundTransaction> findByRefundRequestId(String refundRequestId);
}
