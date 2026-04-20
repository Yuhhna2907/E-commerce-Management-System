package com.codegym.smartphonemanagement.service.profile;

import com.codegym.smartphonemanagement.model.OrderStatus;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.UserAddress;
import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.util.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;

    // ===================== PROFILE =====================

    @Override
    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

        // Stats - optimized with aggregate queries
        long totalOrders = orderRepository.countByUserId(userId);
        long totalWishlists = wishlistRepository.countByUserId(userId);

        // Review count - optimized with aggregate query
        long reviewCount = reviewRepository.countByUserId(userId);

        // Total spent (non-cancelled) - optimized with aggregate query
        BigDecimal totalSpent = orderRepository.sumTotalPriceByUserIdAndStatusNot(userId, OrderStatus.CANCELLED);

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .createdAt(user.getCreatedAt())
                .totalOrders(totalOrders)
                .totalWishlists(totalWishlists)
                .totalReviews(reviewCount)
                .totalSpent(totalSpent)
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

        if (dto.getFullName() != null) {
            validateFullName(dto.getFullName());
            user.setFullName(dto.getFullName());
        }
        
        if (dto.getEmail() != null) {
            validateEmail(dto.getEmail());
            user.setEmail(dto.getEmail());
        }
        
        if (dto.getPhone() != null) {
            validatePhone(dto.getPhone());
            user.setPhone(dto.getPhone());
        }
        
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());

        userRepository.save(user);
    }

    // ===================== ADDRESS BOOK =====================

    @Override
    public List<UserAddressDTO> getAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                .stream()
                .map(UserProfileMapper::toAddressDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressDTO addAddress(Long userId, UserAddressRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

        // Validate address input
        validateAddressInput(dto);

        // GIỚI HẠN 5 ĐỊA CHỈ - optimized with aggregate query
        long count = userAddressRepository.countByUserId(userId);
        if (count >= 5) {
            throw new BadRequestException("Bạn chỉ được lưu tối đa 5 địa chỉ. Hãy xóa bớt địa chỉ cũ nhé!");
        }

        boolean shouldSetDefault = Boolean.TRUE.equals(dto.getIsDefault())
                || !userAddressRepository.existsByUserId(userId);
        if (shouldSetDefault) {
            userAddressRepository.clearDefaultByUserId(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .label(dto.getLabel() != null && !dto.getLabel().isBlank() ? dto.getLabel() : "Địa chỉ mới")
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .addressDetail(dto.getAddressDetail())
                .ward(dto.getWard())
                .district(dto.getDistrict())
                .province(dto.getProvince())
                .isDefault(shouldSetDefault)
                .build();

        return UserProfileMapper.toAddressDTO(userAddressRepository.save(address));
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, Long addressId, UserAddressRequestDTO dto) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        // Validate address input
        validateAddressInput(dto);

        address.setLabel(dto.getLabel() != null && !dto.getLabel().isBlank() ? dto.getLabel() : address.getLabel());
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setAddressDetail(dto.getAddressDetail());
        address.setWard(dto.getWard());
        address.setDistrict(dto.getDistrict());
        address.setProvince(dto.getProvince());

        // Nếu set làm mặc định mới
        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            userAddressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        }

        userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        userAddressRepository.clearDefaultByUserId(userId);

        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        // FIX #9: Sử dụng pessimistic lock để tránh race condition
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        // Nếu xóa địa chỉ mặc định, set địa chỉ đầu tiên còn lại làm mặc định
        if (wasDefault) {
            userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                    .stream().findFirst().ifPresent(first -> {
                        first.setIsDefault(true);
                        userAddressRepository.save(first);
                    });
        }
    }

    @Override
    public UserAddressDTO getDefaultAddress(Long userId) {
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(UserProfileMapper::toAddressDTO)
                .orElse(null);
    }

    // ===================== VALIDATION HELPERS =====================

    private void validateFullName(String fullName) {
        if (fullName.isBlank()) {
            throw new BadRequestException("Họ tên không được để trống");
        }
        if (fullName.length() > 100) {
            throw new BadRequestException("Họ tên không được quá 100 ký tự");
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank()) {
            throw new BadRequestException("Email không được để trống");
        }
        // Simple email regex pattern
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BadRequestException("Email không hợp lệ");
        }
        if (email.length() > 100) {
            throw new BadRequestException("Email không được quá 100 ký tự");
        }
    }

    private void validatePhone(String phone) {
        if (phone.isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        // Vietnamese phone number: starts with 0, followed by 9 digits
        if (!phone.matches("^0\\d{9}$")) {
            throw new BadRequestException("Số điện thoại không hợp lệ (phải có 10 số và bắt đầu bằng 0)");
        }
    }

    private void validateAddressInput(UserAddressRequestDTO dto) {
        // Validate receiver name
        if (dto.getReceiverName() == null || dto.getReceiverName().isBlank()) {
            throw new BadRequestException("Tên người nhận không được để trống");
        }
        if (dto.getReceiverName().length() > 100) {
            throw new BadRequestException("Tên người nhận không được quá 100 ký tự");
        }

        // Validate receiver phone
        if (dto.getReceiverPhone() == null || dto.getReceiverPhone().isBlank()) {
            throw new BadRequestException("Số điện thoại người nhận không được để trống");
        }
        if (!dto.getReceiverPhone().matches("^0\\d{9}$")) {
            throw new BadRequestException("Số điện thoại người nhận không hợp lệ (phải có 10 số và bắt đầu bằng 0)");
        }

        // Validate address detail
        if (dto.getAddressDetail() == null || dto.getAddressDetail().isBlank()) {
            throw new BadRequestException("Địa chỉ chi tiết không được để trống");
        }
        if (dto.getAddressDetail().length() > 255) {
            throw new BadRequestException("Địa chỉ chi tiết không được quá 255 ký tự");
        }

        // Validate ward
        if (dto.getWard() == null || dto.getWard().isBlank()) {
            throw new BadRequestException("Phường/Xã không được để trống");
        }
        if (dto.getWard().length() > 100) {
            throw new BadRequestException("Phường/Xã không được quá 100 ký tự");
        }

        // Validate district
        if (dto.getDistrict() == null || dto.getDistrict().isBlank()) {
            throw new BadRequestException("Quận/Huyện không được để trống");
        }
        if (dto.getDistrict().length() > 100) {
            throw new BadRequestException("Quận/Huyện không được quá 100 ký tự");
        }

        // Validate province
        if (dto.getProvince() == null || dto.getProvince().isBlank()) {
            throw new BadRequestException("Tỉnh/Thành phố không được để trống");
        }
        if (dto.getProvince().length() > 100) {
            throw new BadRequestException("Tỉnh/Thành phố không được quá 100 ký tự");
        }

        // Validate label (optional field)
        if (dto.getLabel() != null && dto.getLabel().length() > 50) {
            throw new BadRequestException("Nhãn địa chỉ không được quá 50 ký tự");
        }
    }
}
