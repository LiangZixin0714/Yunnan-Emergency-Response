package com.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EmergencyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyPlatformApplication.class, args);
    }
}