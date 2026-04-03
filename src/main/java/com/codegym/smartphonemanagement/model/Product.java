package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;

    @NotBlank(message = "Hãng không được trống")
    private String brand;

    @NotNull(message = "Giá không được null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải > 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock không được âm")
    private Integer stock;

    @Column(nullable = false)
    private Boolean active = true;

    private String storage;
    private String color;

    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}