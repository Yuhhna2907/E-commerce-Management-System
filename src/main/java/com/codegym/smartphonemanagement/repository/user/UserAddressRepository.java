package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.UserAddress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    // FIX #9: Method với pessimistic lock để tránh race condition khi delete address
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAddress u WHERE u.id = :id AND u.user.id = :userId")
    Optional<UserAddress> findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Transactional
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
