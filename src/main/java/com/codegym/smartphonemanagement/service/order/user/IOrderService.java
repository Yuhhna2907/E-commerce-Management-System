package com.codegym.smartphonemanagement.service.order.user;

import com.codegym.smartphonemanagement.service.order.DTO.OrderResponseDTO;

import java.util.List;

public interface IOrderService {
    OrderResponseDTO createOrder(Long userId, String receiverName, String receiverPhone,
                                 String shippingAddress, String note);

    OrderResponseDTO getOrderById(Long id);

    List<OrderResponseDTO> getOrderHistory(Long userId);
}
