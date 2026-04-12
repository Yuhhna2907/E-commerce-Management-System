package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
