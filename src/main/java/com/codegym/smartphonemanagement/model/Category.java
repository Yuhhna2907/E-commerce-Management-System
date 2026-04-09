package com.codegym.smartphonemanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên danh mục không được trống")
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @JsonIgnore // Chống vòng lặp vô tận khi render JSON
    @OneToMany(mappedBy = "category", fetch = FetchType.EAGER) // EAGER để lấy được size() ở HTML
    private List<Product> products;
}