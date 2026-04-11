package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_specifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecification {
    
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    // Hiệu năng
    @Column(name = "processor")
    private String processor; // vd: "Apple M4", "Snapdragon 8 Gen 3"

    @Column(name = "antutu_score")
    private Integer antutuScore; // Điểm hiệu năng định lượng

    // Trải nghiệm nhìn
    @Column(name = "screen_size")
    private Double screenSize; // inch

    @Column(name = "screen_tech")
    private String screenTech; // vd: "OLED 120Hz"

    // Nhiếp ảnh
    @Column(name = "camera_info")
    private String cameraInfo;

    // Năng lượng
    @Column(name = "battery_capacity")
    private Integer batteryCapacity; // mAh (vd: 5000)

    @Column(name = "charging_speed")
    private Integer chargingSpeed; // Watts (vd: 120)

    // Khác
    @Column(name = "os")
    private String os; // iOS 18, Windows 11

    @Column(name = "weight")
    private Double weight; // grams
}
