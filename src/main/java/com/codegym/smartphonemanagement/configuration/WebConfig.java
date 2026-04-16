package com.codegym.smartphonemanagement.configuration;

import com.codegym.smartphonemanagement.config.AdminSecurityInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminSecurityInterceptor adminSecurityInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Ánh xạ link /images/** vào thư mục static/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // Ánh xạ các static resources khác (CSS, JS, etc.)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminSecurityInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/login", "/css/**", "/js/**", "/images/**");
    }
}