package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Coupon;
import com.codegym.smartphonemanagement.model.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    
    List<Coupon> findByStatus(CouponStatus status);
    
    /**
     * Find coupon by code with applicableProducts eagerly loaded to avoid N+1 query
     */
    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE c.code = :code")
    Optional<Coupon> findByCodeWithProducts(@Param("code") String code);
}
