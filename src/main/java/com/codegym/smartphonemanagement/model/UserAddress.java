package com.codegym.smartphonemanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 50)
    private String label; // "Nhà riêng", "Văn phòng"...

    @NotBlank
    @Size(max = 100)
    private String receiverName;

    @NotBlank
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?(([3-9])(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3}))$|^(0|\\+84)[0-9]{9}$")
    private String receiverPhone;

    @Size(max = 255)
    private String addressDetail;

    @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String province;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;
}
