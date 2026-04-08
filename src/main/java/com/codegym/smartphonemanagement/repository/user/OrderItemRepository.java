package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Interface này tạm thời chưa cần viết hàm gì thêm,
    // JpaRepository đã có sẵn các hàm save(), saveAll() Duy cần rồi.
}