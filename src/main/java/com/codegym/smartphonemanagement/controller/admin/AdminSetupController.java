package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.Role;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.RoleRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSetupController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/setup")
    public String setupAdmin() {
        try {
            // Tạo role ADMIN nếu chưa có
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
            if (adminRole == null) {
                adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                roleRepository.save(adminRole);
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
            } else {
                admin.setEnabled(true);
            }

            Set<Role> roles = admin.getRoles() != null ? new HashSet<>(admin.getRoles()) : new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);

            return "Admin user setup completed. Existing admin credentials were preserved.";
        } catch (Exception e) {
            return "Error setting up admin: " + e.getMessage();
        }
    }
}
