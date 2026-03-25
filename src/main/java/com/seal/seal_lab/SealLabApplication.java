package com.seal.seal_lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SealLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SealLabApplication.class, args);
    }

}
