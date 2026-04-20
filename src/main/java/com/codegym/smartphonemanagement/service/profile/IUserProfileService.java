package com.codegym.smartphonemanagement.service.profile;

import com.codegym.smartphonemanagement.model.dto.*;
import java.util.List;

public interface IUserProfileService {

    UserProfileDTO getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequestDTO dto);

    // Address Book
    List<UserAddressDTO> getAddresses(Long userId);

    UserAddressDTO addAddress(Long userId, UserAddressRequestDTO dto);

    void setDefaultAddress(Long userId, Long addressId);

    void deleteAddress(Long userId, Long addressId);

    void updateAddress(Long userId, Long addressId, UserAddressRequestDTO dto);

    UserAddressDTO getDefaultAddress(Long userId);
}
