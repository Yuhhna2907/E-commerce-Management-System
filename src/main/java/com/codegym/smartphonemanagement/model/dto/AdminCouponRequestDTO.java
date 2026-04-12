package com.codegym.smartphonemanagement.model.dto;

import com.codegym.smartphonemanagement.model.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminCouponRequestDTO {
    private Long id;

    @NotBlank(message = "Mã không được trống")
    private String code;
    
    @NotNull(message = "Loại giảm giá bắt buộc")
    private DiscountType discountType;
    
    @NotNull(message = "Giá trị giảm giá bắt buộc")
    @DecimalMin(value = "0.0")
    private BigDecimal discountValue;
    
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Integer maxUsageGlobal;
    private Integer perUserLimit;
    
    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    private LocalDateTime startDate;
    
    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    private LocalDateTime endDate;
    
    private String description;
}
