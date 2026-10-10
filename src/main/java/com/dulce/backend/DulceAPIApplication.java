package com.dulce.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DulceAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(DulceAPIApplication.class, args);
    }
}
