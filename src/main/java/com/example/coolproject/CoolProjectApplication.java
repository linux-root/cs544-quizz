package com.example.coolproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoolProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoolProjectApplication.class, args);
    }

} 