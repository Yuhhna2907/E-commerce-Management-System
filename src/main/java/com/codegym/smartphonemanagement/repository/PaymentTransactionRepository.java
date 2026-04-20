package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.PaymentTransaction;
import com.codegym.smartphonemanagement.model.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentTransaction entity.
 * Provides query methods for retrieving payment transaction records by various criteria.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find payment transaction by order ID
     * 
     * @param orderId the order ID to search for
     * @return Optional containing the payment transaction if found
     */
    Optional<PaymentTransaction> findByOrderId(Long orderId);

    /**
     * Find payment transaction by VNPay transaction number
     * 
     * @param vnpayTransactionNo the VNPay transaction number to search for
     * @return Optional containing the payment transaction if found
     */
    Optional<PaymentTransaction> findByVnpayTransactionNo(String vnpayTransactionNo);

    /**
     * Find all payment transactions with a specific status
     * 
     * @param status the payment transaction status to filter by
     * @return List of payment transactions with the specified status
     */
    List<PaymentTransaction> findByStatus(PaymentTransactionStatus status);

    /**
     * Find all payment transactions created within a date range
     * 
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return List of payment transactions created within the specified date range
     */
    List<PaymentTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
