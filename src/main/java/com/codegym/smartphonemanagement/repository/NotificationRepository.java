package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Notification;
import com.codegym.smartphonemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
    
    void deleteByCreatedAtBefore(java.time.LocalDateTime expiryDate);

    @Modifying
    @Query(value = "UPDATE notifications SET related_url = REPLACE(related_url, '/user/orders/', '/user/order/detail/') WHERE related_url LIKE '/user/orders/%'", nativeQuery = true)
    int fixOldNotificationUrls();
}
