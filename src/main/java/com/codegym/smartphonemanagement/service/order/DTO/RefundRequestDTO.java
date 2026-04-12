package com.codegym.smartphonemanagement.service.order.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {
    private Long orderId;

    @NotBlank(message = "Vui lòng nhập lý do hoàn trả")
    private String reason;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 sản phẩm cần hoàn trả")
    private List<RefundItemDTO> items;
}
