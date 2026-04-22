package com.codegym.smartphonemanagement.configuration;

import com.codegym.smartphonemanagement.config.CacheControlFilter;
import com.codegym.smartphonemanagement.config.CustomAccessDeniedHandler;
import com.codegym.smartphonemanagement.config.CustomAuthenticationEntryPoint;
import com.codegym.smartphonemanagement.config.CustomAuthenticationFailureHandler;
import com.codegym.smartphonemanagement.config.CustomAuthenticationSuccessHandler;
import com.codegym.smartphonemanagement.config.CustomLogoutSuccessHandler;
import com.codegym.smartphonemanagement.config.CustomInvalidSessionStrategy;
import com.codegym.smartphonemanagement.config.CustomSessionInformationExpiredStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // PHẢI CÓ DÒNG NÀY: Để @RequiredArgsConstructor tiêm Service của Duy vào
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CacheControlFilter cacheControlFilter;
    private final CustomSessionInformationExpiredStrategy customSessionInformationExpiredStrategy;
    private final CustomInvalidSessionStrategy customInvalidSessionStrategy;
    private final DataSource dataSource;
    
    @Value("${app.security.remember-me.key}")
    private String rememberMeKey;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // PHẢI CÓ BEAN NÀY: Để kết nối Service và PasswordEncoder lại với nhau
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Bean for persistent token repository used by Remember-Me functionality.
     * Uses JDBC to store tokens in the persistent_logins table.
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }
    
    /**
     * Bean for session registry to track concurrent sessions.
     * Required for session management and concurrent session control.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Ignore CSRF for public API endpoints if needed
                        .ignoringRequestMatchers("/api/public/**")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ACCESS - No authentication required
                        // Authentication pages
                        .requestMatchers("/login", "/register", "/forgot-password", "/reset-password").permitAll()
                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Product browsing (browse products free)
                        .requestMatchers("/", "/user/products", "/user/products/**", "/user/products/search", "/user/products/compare").permitAll()
                        // Public APIs
                        .requestMatchers("/api/products/**", "/api/categories/**", "/api/admin/setup").permitAll()
                        // Error pages
                        .requestMatchers("/error/**").permitAll()
                        
                        // ADMIN ACCESS - Requires ROLE_ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/seller/**").hasRole("ADMIN")
                        
                        // AUTHENTICATED ACCESS - Requires login (purchase requires login)
                        // Shopping features
                        .requestMatchers("/user/cart/**", "/user/checkout/**", "/user/orders/**").authenticated()
                        // User features
                        .requestMatchers("/user/wishlist/**", "/user/saved/**", "/user/profile/**", 
                                "/user/reviews/**", "/user/notifications/**", "/user/loyalty/**", 
                                "/user/addresses/**").authenticated()
                        // User APIs
                        .requestMatchers("/api/user/**").authenticated()
                        
                        // DEFAULT - Deny all other requests
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenRepository(persistentTokenRepository())
                        .tokenValiditySeconds(14 * 24 * 60 * 60) // 14 days
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("remember-me")
                )
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession()) // Migrate session on authentication
                        .invalidSessionStrategy(customInvalidSessionStrategy)
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false) // Invalidate oldest session instead of preventing login
                        .expiredSessionStrategy(customSessionInformationExpiredStrategy)
                        .sessionRegistry(sessionRegistry())
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://fonts.googleapis.com; " +
                                        "img-src 'self' data: https: blob:; " +
                                        "font-src 'self' data: https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                                        "connect-src 'self';")
                        )
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> contentType.disable())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                // THÊM DÒNG NÀY để ép Security dùng Provider Duy vừa tạo ở trên
                .authenticationProvider(authenticationProvider())
                .addFilterAfter(cacheControlFilter, org.springframework.security.web.authentication.logout.LogoutFilter.class);

        return http.build();
    }
}
