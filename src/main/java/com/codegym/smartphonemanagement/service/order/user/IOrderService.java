package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;

import java.util.List;

public interface IOrderService {
    OrderResponseDTO checkout(Long userId);
    List<OrderResponseDTO> getOrderHistory(Long userId);
    OrderResponseDTO getOrderById(Long orderId);
}
