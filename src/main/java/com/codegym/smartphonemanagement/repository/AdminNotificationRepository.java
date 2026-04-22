package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    
    List<AdminNotification> findAllByOrderByCreatedAtDesc();
    
    List<AdminNotification> findTop10ByOrderByCreatedAtDesc();
    
    List<AdminNotification> findByIsReadFalseOrderByCreatedAtDesc();
    
    long countByIsReadFalse();
    
    @Modifying
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(Long id);
    
    @Modifying
    @Query("UPDATE AdminNotification n SET n.isRead = true")
    void markAllAsRead();
}
