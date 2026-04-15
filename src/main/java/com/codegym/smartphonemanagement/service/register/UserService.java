package com.codegym.smartphonemanagement.service.register;

import com.codegym.smartphonemanagement.model.Role;
import com.codegym.smartphonemanagement.model.User;
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

    public void register(User user) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username đã tồn tại: " + user.getUsername());
        }

        // Kiểm tra email đã tồn tại (nếu email không null)
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty() && userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã tồn tại: " + user.getEmail());
        }

        // 1. Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Gán Role mặc định (phải có ROLE_USER trong DB trước)
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Chưa có ROLE_USER trong hệ thống"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        userRepository.save(user);
    }
}
