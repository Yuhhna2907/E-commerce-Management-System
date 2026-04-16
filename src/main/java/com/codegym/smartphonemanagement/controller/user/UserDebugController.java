package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserDebugController {

    private final UserRepository userRepository;

    @GetMapping("/current-info")
    public String getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Chưa đăng nhập";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return "User không tồn tại: " + username;
        }

        return "Username: " + user.getUsername() + 
               ", FullName: " + user.getFullName() + 
               ", Email: " + user.getEmail() + 
               ", Phone: " + user.getPhone() + 
               ", Address: " + user.getAddress() + 
               ", Enabled: " + user.isEnabled() +
               ", Roles: " + user.getRoles();
    }
}

