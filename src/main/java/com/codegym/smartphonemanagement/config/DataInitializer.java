package com.codegym.smartphonemanagement.config;

import com.codegym.smartphonemanagement.model.Role;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.RoleRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DataInitializer: Bắt đầu khởi tạo dữ liệu ===");

        // Tạo role ADMIN nếu chưa có
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
            System.out.println("=== DataInitializer: Đã tạo ROLE_ADMIN ===");
        } else {
            System.out.println("=== DataInitializer: ROLE_ADMIN đã tồn tại ===");
        }

        // Tạo role USER nếu chưa có
        Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
            System.out.println("=== DataInitializer: Đã tạo ROLE_USER ===");
        } else {
            System.out.println("=== DataInitializer: ROLE_USER đã tồn tại ===");
        }

        // Tạo hoặc cập nhật user admin
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .email("admin@smartzone.com")
                    .phone("0123456789")
                    .enabled(true)
                    .build();

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("=== DataInitializer: Đã tạo admin user với username: admin, password: admin123 ===");
        } else {
            // Cập nhật password và roles nếu cần
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            userRepository.save(admin);
            System.out.println("=== DataInitializer: Đã cập nhật admin user với password: admin123 ===");
        }

        System.out.println("=== DataInitializer: Hoàn thành khởi tạo dữ liệu ===");
    }
}
