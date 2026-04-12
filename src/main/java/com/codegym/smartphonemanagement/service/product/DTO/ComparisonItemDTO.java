package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonItemDTO {
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal price;

    // Hiệu năng
    private String processor;
    private Integer antutuScore;
    private boolean bestAntutu;

    // Trải nghiệm nhìn
    private Double screenSize;
    private String screenTech;
    private boolean bestScreenSize;

    // Nhiếp ảnh
    private String cameraInfo;

    // Năng lượng
    private Integer batteryCapacity;
    private boolean bestBattery;

    private Integer chargingSpeed;
    private boolean bestCharging;

    // Khác
    private String os;
    
    private Double weight;
    private boolean lightestWeight;
}
