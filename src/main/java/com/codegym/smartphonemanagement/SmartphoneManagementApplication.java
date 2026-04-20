package com.codegym.smartphonemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartphoneManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartphoneManagementApplication.class, args);
    }

}
