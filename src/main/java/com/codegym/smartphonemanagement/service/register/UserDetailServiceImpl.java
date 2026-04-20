package com.codegym.smartphonemanagement.service.register;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.security.LoginAttemptService;
import com.codegym.smartphonemanagement.util.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user details for username or email: {}", usernameOrEmail);

        // Check if account is locked before loading user
        if (loginAttemptService.isAccountLocked(usernameOrEmail)) {
            log.warn("Login attempt for locked account: {}", usernameOrEmail);
            throw new UsernameNotFoundException("Account is temporarily locked due to multiple failed login attempts");
        }

        // Try to find user by username first, then by email
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> {
                    log.warn("User not found: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User không tồn tại: " + usernameOrEmail);
                });

        log.debug("User found: {}, enabled: {}", user.getUsername(), user.isEnabled());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .accountExpired(false)
                .accountLocked(user.isAccountLocked())
                .credentialsExpired(false)
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()))
                .build();
    }
}