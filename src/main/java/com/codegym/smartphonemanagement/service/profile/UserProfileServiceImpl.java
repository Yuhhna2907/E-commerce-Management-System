package com.codegym.smartphonemanagement.service.profile;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.UserAddress;
import com.codegym.smartphonemanagement.model.dto.*;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
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

        // Stats
        long totalOrders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        long totalWishlists = wishlistRepository.findByUserIdOrderByAddedAtDesc(userId).size();

        // Review count
        long reviewCount = reviewRepository.findAll().stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId))
                .count();

        // Total spent (non-cancelled)
        BigDecimal totalSpent = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getStatus() != com.codegym.smartphonemanagement.model.OrderStatus.CANCELLED)
                .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

        // Vì chưa có Security (BCrypt), so sánh plain text tạm thời
        if (!dto.getCurrentPassword().equals(user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận không khớp");
        }
        
        if (dto.getNewPassword().length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        user.setPassword(dto.getNewPassword());
        userRepository.save(user);
    }

    // ===================== ADDRESS BOOK =====================

    @Override
    public List<UserAddressDTO> getAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                .stream().map(this::mapToAddressDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressDTO addAddress(Long userId, UserAddressRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

        // GIỚI HẠN 5 ĐỊA CHỈ
        long count = userAddressRepository.findByUserIdOrderByIsDefaultDesc(userId).size();
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

        return mapToAddressDTO(userAddressRepository.save(address));
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, Long addressId, UserAddressRequestDTO dto) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

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
        userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        userAddressRepository.clearDefaultByUserId(userId);

        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId).get();
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
                .map(this::mapToAddressDTO).orElse(null);
    }

    private UserAddressDTO mapToAddressDTO(UserAddress a) {
        return UserAddressDTO.builder()
                .id(a.getId())
                .label(a.getLabel())
                .receiverName(a.getReceiverName())
                .receiverPhone(a.getReceiverPhone())
                .addressDetail(a.getAddressDetail())
                .ward(a.getWard())
                .district(a.getDistrict())
                .province(a.getProvince())
                .isDefault(a.getIsDefault())
                .build();
    }
}
