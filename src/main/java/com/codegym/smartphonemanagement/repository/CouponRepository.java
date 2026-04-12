package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Coupon;
import com.codegym.smartphonemanagement.model.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    List<Coupon> findByStatus(CouponStatus status);
}
