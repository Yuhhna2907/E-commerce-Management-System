package com.codegym.smartphonemanagement.service.product.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecificationDTO {
    private String processor;
    private Integer antutuScore;
    private Double screenSize;
    private String screenTech;
    private String cameraInfo;
    private Integer batteryCapacity;
    private Integer chargingSpeed;
    private String os;
    private Double weight;
}
