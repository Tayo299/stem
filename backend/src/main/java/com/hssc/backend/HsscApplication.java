package com.hssc.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HsscApplication {
    public static void main(String[] args) {
        SpringApplication.run(HsscApplication.class, args);
    }
}
