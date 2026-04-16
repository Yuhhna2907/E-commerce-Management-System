package com.codegym.smartphonemanagement.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ánh xạ link /uploads/** vào thư mục vật lý trong project root
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        
        // Ánh xạ link /images/** vào thư mục static/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        // Ánh xạ các static resources khác (CSS, JS, etc.)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}