package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.SzWithdrawalRequest;
import com.codegym.smartphonemanagement.model.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SzWithdrawalRequestRepository extends JpaRepository<SzWithdrawalRequest, Long> {

    List<SzWithdrawalRequest> findByStatusOrderByCreatedAtAsc(WithdrawalStatus status);

    Page<SzWithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByStatus(WithdrawalStatus status);
}
