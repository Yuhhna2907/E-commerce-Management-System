package com.codegym.smartphonemanagement.service.register;

import com.codegym.smartphonemanagement.model.Role;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.dto.UserRegistrationDTO;
import com.codegym.smartphonemanagement.repository.RoleRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(UserRegistrationDTO dto) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại: " + dto.getUsername());
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại: " + dto.getEmail());
        }

        // Tạo User entity từ DTO
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setFullName(dto.getFullName());

        // Gán Role mặc định (phải có ROLE_USER trong DB trước)
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Chưa có ROLE_USER trong hệ thống"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        userRepository.save(user);
    }
}
