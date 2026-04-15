package com.codegym.smartphonemanagement.service.register;

import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Sửa trong UserDetailServiceImpl.java
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("===> Spring Security đang kiểm tra username: " + username); // THÊM DÒNG NÀY

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("===> KHÔNG TÌM THẤY USER: " + username); // THÊM DÒNG NÀY
                    return new UsernameNotFoundException("User không tồn tại: " + username);
                });

        System.out.println("===> Mật khẩu trong DB: " + user.getPassword()); // THÊM DÒNG NÀY

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled()) // false nghĩa là tài khoản ĐANG HOẠT ĐỘNG
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList()))
                .build();
    }
}