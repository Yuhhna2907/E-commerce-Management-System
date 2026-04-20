package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.SzWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface SzWalletRepository extends JpaRepository<SzWallet, Long> {

    Optional<SzWallet> findByUserId(Long userId);

    /** Tổng tiền đang giữ hộ trong tất cả ví (dùng cho Admin stats) */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM SzWallet w")
    BigDecimal sumAllBalances();

    /** Đếm số ví đang có tiền */
    @Query("SELECT COUNT(w) FROM SzWallet w WHERE w.balance > 0")
    long countWalletsWithBalance();
}
