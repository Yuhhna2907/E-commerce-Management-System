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
    public void run(String... args) {
        System.out.println("=== DataInitializer: Bat dau khoi tao du lieu ===");

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            return roleRepository.save(role);
        });

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_USER");
            return roleRepository.save(role);
        });

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
            System.out.println("=== DataInitializer: Da tao admin user mac dinh ===");
        } else {
            admin.setEnabled(true);
            Set<Role> roles = admin.getRoles() != null ? new HashSet<>(admin.getRoles()) : new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            userRepository.save(admin);
            System.out.println("=== DataInitializer: Da xac nhan admin user ton tai va co ROLE_ADMIN ===");
        }

        System.out.println("=== DataInitializer: ROLE_USER san sang: " + (userRole.getId() != null) + " ===");
        System.out.println("=== DataInitializer: Hoan thanh khoi tao du lieu ===");
    }
}
