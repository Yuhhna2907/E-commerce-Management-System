package com.codegym.smartphonemanagement.service.order.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundItemDTO {
    private Long orderItemId;
    private Integer quantity;
}
