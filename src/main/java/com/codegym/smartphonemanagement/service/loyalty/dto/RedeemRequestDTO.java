package com.codegym.smartphonemanagement.service.loyalty.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedeemRequestDTO {
    @Min(value = 100, message = "Cần tối thiểu 100 điểm để đổi ưu đãi")
    private Integer points;
}
