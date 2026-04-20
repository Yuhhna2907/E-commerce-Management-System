package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.SzWalletTransaction;
import com.codegym.smartphonemanagement.model.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SzWalletTransactionRepository extends JpaRepository<SzWalletTransaction, Long> {

    Page<SzWalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    List<SzWalletTransaction> findTop10ByOrderByCreatedAtDesc();

    /** Tổng tiền của một loại giao dịch trong khoảng thời gian (cho stats) */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SzWalletTransaction t " +
           "WHERE t.type = :type AND t.createdAt >= :from AND t.createdAt <= :to")
    BigDecimal sumByTypeAndPeriod(@Param("type") WalletTransactionType type,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
