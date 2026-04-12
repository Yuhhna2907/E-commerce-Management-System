package com.codegym.smartphonemanagement.model.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO {
    private Long id;
    private String label;
    private String receiverName;
    private String receiverPhone;
    private String addressDetail;
    private String ward;
    private String district;
    private String province;
    private Boolean isDefault;

    public String getFullAddress() {
        return String.join(", ",
                addressDetail != null ? addressDetail : "",
                ward != null ? ward : "",
                district != null ? district : "",
                province != null ? province : "");
    }
}
