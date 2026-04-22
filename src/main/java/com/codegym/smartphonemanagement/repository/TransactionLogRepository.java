package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.TransactionLog;
import com.codegym.smartphonemanagement.model.TransactionLogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    Page<TransactionLog> findByTransactionReferenceContainingIgnoreCase(String transactionReference, Pageable pageable);
    
    Page<TransactionLog> findByType(TransactionLogType type, Pageable pageable);

    Page<TransactionLog> findByTransactionReferenceContainingIgnoreCaseAndType(String transactionReference, TransactionLogType type, Pageable pageable);

}
