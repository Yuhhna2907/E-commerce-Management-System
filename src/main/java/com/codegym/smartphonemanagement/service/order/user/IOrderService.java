package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.service.order.DTO.OrderRequestDTO;
import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;

import java.util.List;

public interface IOrderService {
    OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderDTO);

    OrderResponseDTO getOrderById(Long id);

    List<OrderResponseDTO> getOrderHistory(Long userId);
}
