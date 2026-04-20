package com.codegym.smartphonemanagement.util;

import com.codegym.smartphonemanagement.model.UserAddress;
import com.codegym.smartphonemanagement.model.dto.UserAddressDTO;
import lombok.experimental.UtilityClass;

/**
 * Utility class for mapping UserProfile-related entities to DTOs.
 * This class provides static methods for converting domain models to data transfer objects.
 */
@UtilityClass
public class UserProfileMapper {

    /**
     * Maps a UserAddress entity to UserAddressDTO.
     *
     * @param address the UserAddress entity to map
     * @return UserAddressDTO containing the address information
     */
    public static UserAddressDTO toAddressDTO(UserAddress address) {
        if (address == null) {
            return null;
        }

        return UserAddressDTO.builder()
                .id(address.getId())
                .label(address.getLabel())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .addressDetail(address.getAddressDetail())
                .ward(address.getWard())
                .district(address.getDistrict())
                .province(address.getProvince())
                .isDefault(address.getIsDefault())
                .build();
    }
}
