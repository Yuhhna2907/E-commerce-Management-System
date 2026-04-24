package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.UserWallet;
import com.codegym.smartphonemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    List<UserWallet> findByUser(User user);
    Optional<UserWallet> findByUserIdAndCouponId(Long userId, Long couponId);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    @Query("SELECT COUNT(uw) FROM UserWallet uw")
    long countTotalSaved();
}
